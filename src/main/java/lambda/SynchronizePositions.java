package lambda;

import application.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.database.RobinhoodOrder;
import stockstream.database.VotedOrder;
import stockstream.database.VotedOrderRegistry;
import stockstream.logic.Scheduler;
import util.TimeUtil;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SynchronizePositions {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private TimeUtil timeUtil;

    @Autowired
    private VotedOrderRegistry votedOrderRegistry;


    @PostConstruct
    public void init() {
        scheduler.scheduleJob(() -> timeUtil.runIfMarketOpen(this::synchronizeVotedOrders), 0, Config.QUICK_REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void synchronizeVotedOrders() {
        final String dateStr = stockstream.util.TimeUtil.getCanonicalYMDString(new Date());
        final List<RobinhoodOrder> newRobinhoodOrders = new ArrayList<>(votedOrderRegistry.findUnmergedRobinhoodOrders(dateStr));

        newRobinhoodOrders.sort((o1, o2) -> {
            final String order1Timestamp = o1.computeRankedTimestamp();
            final String order2Timestamp = o2.computeRankedTimestamp();
            return order1Timestamp.compareTo(order2Timestamp);
        });

        for (final RobinhoodOrder robinhoodOrder : newRobinhoodOrders) {
            final VotedOrder votedOrder = new VotedOrder(robinhoodOrder);
            votedOrderRegistry.saveVotedOrder(votedOrder);
        }

        log.info("{} orders synced.", newRobinhoodOrders.size());
    }

}
