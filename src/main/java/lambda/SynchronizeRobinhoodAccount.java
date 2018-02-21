package lambda;

import application.Config;
import com.cheddar.robinhood.client.RobinhoodClient;
import com.cheddar.robinhood.data.MarginBalances;
import com.cheddar.robinhood.data.Portfolio;
import com.cheddar.robinhood.exception.RobinhoodException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.database.RobinhoodAccountRegistry;
import stockstream.database.RobinhoodAccountStub;
import stockstream.logic.Scheduler;
import util.TimeUtil;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SynchronizeRobinhoodAccount {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private RobinhoodClient robinhoodClient;

    @Autowired
    private RobinhoodAccountRegistry robinhoodAccountRegistry;

    @Autowired
    private TimeUtil timeUtil;

    @PostConstruct
    public void init() {
        scheduler.scheduleJob(() -> timeUtil.runIfMarketOpen(this::refreshAccountInformation), 0, Config.QUICK_REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void refreshAccountInformation() {
        try {
            final Portfolio portfolio = robinhoodClient.getPortfolio();
            final MarginBalances marginBalances = robinhoodClient.getMarginBalances();
            marginBalances.setUnallocated_margin_cash(marginBalances.getUnallocated_margin_cash() - Config.MARGIN_ADJUSTMENT);

            final RobinhoodAccountStub robinhoodAccountStub = new RobinhoodAccountStub(marginBalances, portfolio);

            robinhoodAccountRegistry.saveAccountInfo(robinhoodAccountStub);
        } catch (RobinhoodException e) {
            log.warn(e.getMessage(), e);
        }
    }
}
