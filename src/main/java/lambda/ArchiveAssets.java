package lambda;

import application.Config;
import cache.BrokerCache;
import com.cheddar.robinhood.client.RobinhoodClient;
import com.cheddar.robinhood.data.Position;
import com.cheddar.robinhood.data.Quote;
import com.cheddar.robinhood.exception.RobinhoodException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import spark.utils.StringUtils;
import stockstream.database.Asset;
import stockstream.database.AssetRegistry;
import stockstream.database.InstrumentStub;
import stockstream.logic.Scheduler;
import util.TimeUtil;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class ArchiveAssets {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private TimeUtil timeUtil;

    @Autowired
    private RobinhoodClient robinhoodClient;

    @Autowired
    private AssetRegistry assetRegistry;

    @PostConstruct
    public void init() {
        scheduler.scheduleJob(() -> timeUtil.runIfMarketOpen(this::synchronizeAssets), 0, Config.QUICK_REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private Set<String> findSymbolsForPositions(final Collection<Position> positions) {
        final Set<String> symbols = new HashSet<>();

        for (final Position position : positions) {
            if (StringUtils.isEmpty(position.getInstrument())) {
                continue;
            }
            final Optional<InstrumentStub> instrument = this.brokerCache.getInstrumentForURL(position.getInstrument());
            if (!instrument.isPresent()) {
                continue;
            }
            symbols.add(instrument.get().getSymbol());
        }

        return symbols;
    }

    public List<Asset> getAllAssets() throws RobinhoodException {
        log.info("Getting owned assets.");

        final List<Position> positions = robinhoodClient.getPositions();
        final List<Position> filteredPositions = positions.stream().filter(pos -> pos.getQuantity() > 0).collect(Collectors.toList());

        final Set<String> symbols = findSymbolsForPositions(filteredPositions);
        final List<Quote> quotes = robinhoodClient.getQuotes(symbols);

        final Map<String, Quote> instrumentURLToQuote = new HashMap<>();
        quotes.forEach(quote -> instrumentURLToQuote.put(quote.getInstrument(), quote));

        final List<Asset> ownedAssets = new ArrayList<>();

        for (final Position position : filteredPositions) {
            final double shares = position.getQuantity();
            final double avgBuyPrice = position.getAverage_buy_price();

            final String instrumentURL = position.getInstrument();
            final Optional<InstrumentStub> instrument = this.brokerCache.getInstrumentForURL(position.getInstrument());

            if (!instrument.isPresent() || !instrumentURLToQuote.containsKey(instrumentURL)) {
                continue;
            }

            final String symbol = instrument.get().getSymbol();

            final Quote quote = instrumentURLToQuote.get(instrumentURL);

            ownedAssets.add(new Asset(symbol, Math.toIntExact((long) shares), avgBuyPrice, quote));
        }

        log.info("Got {} assets from Robinhood.", ownedAssets.size());

        return ownedAssets;
    }

    private synchronized void synchronizeAssets() {
        try {

            final List<Asset> orders = getAllAssets();

            assetRegistry.saveAssets(orders);

        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
    }


}
