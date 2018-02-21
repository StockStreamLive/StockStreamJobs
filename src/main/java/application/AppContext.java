package application;

import cache.BrokerCache;
import com.cheddar.robinhood.client.RobinhoodClient;
import lambda.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import stockstream.spring.DatabaseBeans;
import stockstream.spring.LogicBeans;
import util.TimeUtil;

@Import({LogicBeans.class,
         DatabaseBeans.class})
@Configuration
public class AppContext {

    @Bean
    public TimeUtil timeUtil() {
        return new TimeUtil();
    }

    @Bean
    public BrokerCache brokerCache() {
        return new BrokerCache();
    }

    @Bean
    public ArchiveOrders archiveOrders() {
        return new ArchiveOrders();
    }

    @Bean
    public EquityHistoricalArchiver equityHistoricalArchiver() {
        return new EquityHistoricalArchiver();
    }

    @Bean
    public PortfolioSynchronizer portfolioSynchronizer() {
        return new PortfolioSynchronizer();
    }

    @Bean
    public SynchronizeWalletActivity synchronizeWalletActivity() {
        return new SynchronizeWalletActivity();
    }

    @Bean
    public ArchiveInstruments archiveInstruments() {
        return new ArchiveInstruments();
    }

    @Bean
    public SynchronizeRobinhoodAccount synchronizeRobinhoodAccount() {
        return new SynchronizeRobinhoodAccount();
    }

    @Bean
    public ArchiveAssets archiveAssets() {
        return new ArchiveAssets();
    }

    @Bean
    public SynchronizePositions synchronizePositions() {
        return new SynchronizePositions();
    }

    @Bean
    public RobinhoodClient robinhoodClient() {
        return new RobinhoodClient(System.getenv("RH_UN"), System.getenv("RH_PW"));
    }

}
