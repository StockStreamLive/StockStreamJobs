package lambda;

import application.Config;
import cache.BrokerCache;
import com.cheddar.robinhood.client.RobinhoodClient;
import com.cheddar.robinhood.data.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.database.InstrumentStub;
import stockstream.database.RobinhoodOrder;
import stockstream.database.RobinhoodOrderRegistry;
import stockstream.logic.Scheduler;
import util.TimeUtil;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ArchiveOrders {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private TimeUtil timeUtil;

    @Autowired
    private RobinhoodClient robinhoodClient;

    @Autowired
    private RobinhoodOrderRegistry robinhoodOrderRegistry;

    @PostConstruct
    public void init() {
        scheduler.scheduleJob(() -> timeUtil.runIfMarketOpen(() -> synchronizeOrders(timeUtil.getStartOfToday())), 0, Config.REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
        scheduler.scheduleJob(() -> timeUtil.runIfMarketOpen(() -> synchronizeOrders(DateUtils.addSeconds(new Date(), -60))), 0, Config.QUICK_REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private synchronized void synchronizeOrders(final Date afterDate) {
        try {
            final Collection<Order> orders = robinhoodClient.getOrdersAfterDate(afterDate);

            final Set<RobinhoodOrder> robinhoodOrders = new HashSet<>();

            orders.forEach(order -> {
                final Optional<InstrumentStub> instrumentOptional = brokerCache.getInstrumentForURL(order.getInstrument());
                if (!instrumentOptional.isPresent()) {
                    return;
                }

                final String symbol = instrumentOptional.get().getSymbol();

                robinhoodOrders.add(new RobinhoodOrder(symbol, order));
            });

            robinhoodOrderRegistry.saveRobinhoodOrders(robinhoodOrders);
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
    }


}
