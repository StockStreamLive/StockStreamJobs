package lambda;

import application.Config;
import com.cheddar.robinhood.client.RobinhoodClient;
import com.cheddar.robinhood.data.EquityHistorical;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.database.HistoricalEquityValue;
import stockstream.database.HistoricalEquityValueRegistry;
import stockstream.logic.Scheduler;
import util.TimeUtil;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EquityHistoricalArchiver {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private TimeUtil timeUtil;

    @Autowired
    private RobinhoodClient robinhoodClient;

    @Autowired
    private HistoricalEquityValueRegistry historicalEquityValueRegistry;

    @PostConstruct
    public void init() {
        scheduler.scheduleJob(() -> timeUtil.runIfMarketOpen(this::syncPortfolioValue), 0, Config.REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    private void syncPortfolioValue() {
        try {
            final List<EquityHistorical> yearHistoricalValues = robinhoodClient.getHistoricalValues("year", "day", "regular");
            final List<EquityHistorical> todaysHistoricalValues = robinhoodClient.getHistoricalValues("day", "5minute", "extended");

            final EquityHistorical todaysValue = yearHistoricalValues.get(yearHistoricalValues.size() - 1);

            final List<HistoricalEquityValue> historicalEquityValues = new ArrayList<>();
            todaysHistoricalValues.forEach(value -> {
                final HistoricalEquityValue historicalEquityValue = new HistoricalEquityValue();
                historicalEquityValue.setBegins_at(value.getBegins_at());
                historicalEquityValue.setAdjusted_open_equity(value.getAdjusted_open_equity());
                historicalEquityValue.setAdjusted_close_equity(value.getAdjusted_close_equity());
                historicalEquityValues.add(historicalEquityValue);
            });

            historicalEquityValues.add(new HistoricalEquityValue(todaysValue));

            historicalEquityValueRegistry.saveHistoricalEquityValues(historicalEquityValues);
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
    }


}
