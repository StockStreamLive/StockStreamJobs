package lambda;

import application.Config;
import com.cheddar.robinhood.client.RobinhoodClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.database.WalletOrderRegistry;
import stockstream.logic.Scheduler;
import util.TimeUtil;

import javax.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SynchronizeWalletActivity {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private TimeUtil timeUtil;

    @Autowired
    private RobinhoodClient robinhoodClient;

    @Autowired
    private WalletOrderRegistry walletOrderRegistry;

    @PostConstruct
    public void init() {
        scheduler.scheduleJob(() -> timeUtil.runIfMarketOpen(this::synchronizeWalletActivity), 0, Config.REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    private void synchronizeWalletActivity() {
        int ordersRemoved = 0;
        try {
            final Set<String> failedSellOrders = walletOrderRegistry.findUnfilledWalletSellOrders();

            ordersRemoved = failedSellOrders.size();

            if (ordersRemoved > 0) {
                walletOrderRegistry.eraseFailedSellOrders(failedSellOrders);
            }
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }

        log.info("Removed {} orders.", ordersRemoved);
    }

}
