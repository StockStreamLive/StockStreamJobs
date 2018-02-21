package lambda;


import application.Config;
import cache.BrokerCache;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.cheddar.robinhood.client.RobinhoodClient;
import com.cheddar.robinhood.data.MarginBalances;
import com.cheddar.robinhood.data.Order;
import com.cheddar.robinhood.data.Position;
import com.cheddar.robinhood.exception.RobinhoodException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.database.InstrumentStub;
import stockstream.logic.Scheduler;
import stockstream.util.JSONUtil;
import util.TimeUtil;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class PortfolioSynchronizer {

    private static final String API_S3_BUCKET = "api.stockstream.live";

    private static final Set<String> ORDER_STATUS_WHITELIST = ImmutableSet.of("unconfirmed", "confirmed", "queued");

    @Data
    @AllArgsConstructor
    class AssetNode {
        private String symbol;
        private int shares;
        private double avgCost;
    }

    @Data
    @AllArgsConstructor
    class OrderNode {
        private String symbol;
        private String state;
        private String createdAt;
        private String side;
        private float price;
        private int shares;

        public OrderNode(final Order order) {
            this.symbol = brokerCache.getInstrumentForURL(order.getInstrument()).get().getSymbol();
            this.shares = (int)Float.parseFloat(order.getQuantity());
            this.price = Float.parseFloat(order.getPrice());
            this.createdAt = order.getCreated_at();
            this.state = "Pending";
            this.side = order.getSide();
        }
    }

    @Data
    @AllArgsConstructor
    class Account {
        final float cashBalance;
        final float spentMargin;
        final Collection<AssetNode> assets;
        final Collection<OrderNode> orders;
    }

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private TimeUtil timeUtil;

    @Autowired
    private RobinhoodClient robinhoodClient;

    @PostConstruct
    public void init() {
        scheduler.scheduleJob(() -> timeUtil.runIfMarketOpen(this::publishAssets), 0, Config.REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    private void publishAssets() {
        log.info("Publishing assets.");

        Optional<String> accountDataOptional = constructPortfolioJSON();

        if (!accountDataOptional.isPresent()) {
            log.warn("Account message empty, not sending anything.");
            return;
        }

        publishToS3(accountDataOptional.get());
    }

    @VisibleForTesting
    protected double computeCashBalance(final MarginBalances marginBalances) {
        double cashBalance = marginBalances.getUnallocated_margin_cash() - marginBalances.getMargin_limit();

        if (cashBalance < 0) {
            cashBalance = 0;
        }

        return cashBalance;
    }

    @VisibleForTesting
    protected double computeSpentMargin(final MarginBalances marginBalances) {
        double cashMinusMargin = marginBalances.getUnallocated_margin_cash() - marginBalances.getMargin_limit();
        double spentMargin = 0;

        if (cashMinusMargin < 0) {
            spentMargin = Math.abs(marginBalances.getUnallocated_margin_cash() - marginBalances.getMargin_limit());
        }

        return spentMargin;
    }

    private Optional<String> constructPortfolioJSON() {
        Optional<String> assetsMessage = Optional.empty();

        try {
            final MarginBalances marginBalances = robinhoodClient.getMarginBalances();

            double cashBalance = computeCashBalance(marginBalances);
            double spentMargin = computeSpentMargin(marginBalances);

            final List<AssetNode> assets = constructAssetNodes();

            if (assets.size() == 0) {
                return Optional.empty();
            }

            final List<OrderNode> pendingOrders = constructPendingOrderNodes();
            final Account account = new Account((float)cashBalance, (float)spentMargin, assets, pendingOrders);
            assetsMessage = JSONUtil.serializeObject(account, false);

        } catch (final RobinhoodException e) {
            log.warn("Could not get assets from broker!", e);
        }

        return assetsMessage;
    }

    private List<AssetNode> constructAssetNodes() throws RobinhoodException {
        final List<AssetNode> assetNodes = new ArrayList<>();

        final List<Position> positions = robinhoodClient.getPositions();

        for (final Position position : positions) {
            final double shares = position.getQuantity();
            final double avgBuyPrice = position.getAverage_buy_price();

            if (shares <= 0) {
                continue;
            }

            final String instrumentURL = position.getInstrument();
            final Optional<InstrumentStub> instrumentOptional = brokerCache.getInstrumentForURL(instrumentURL);

            if (!instrumentOptional.isPresent()) {
                continue;
            }

            final String symbol = instrumentOptional.get().getSymbol();

            assetNodes.add(new AssetNode(symbol, Math.toIntExact((long) shares), avgBuyPrice));
        }

        return assetNodes;
    }

    private List<OrderNode> constructPendingOrderNodes() throws RobinhoodException {
        return this.robinhoodClient.getOrdersAfterDate(timeUtil.getStartOfToday()).stream().filter(order -> ORDER_STATUS_WHITELIST.contains(order.getState())).map(OrderNode::new).collect(Collectors.toList());
    }

    private void publishToS3(final String accountData) {
        final AmazonS3 s3client = AmazonS3ClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).build();

        final String path = String.format("/tmp/assets-%s", new Date().getTime());
        try {
            Files.write(Paths.get(path), accountData.getBytes());
        } catch (final IOException e) {
            log.error("Could not update portfolio at path: {}", path, e);
            return;
        }

        s3client.putObject(new PutObjectRequest(API_S3_BUCKET, "portfolio/current", new File(path)));
    }

}
