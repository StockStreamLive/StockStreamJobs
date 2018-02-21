package util;

import cache.BrokerCache;
import com.cheddar.robinhood.data.MarketState;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;

@Slf4j
public class TimeUtil {

    private final int MARKET_BUFFER_MINUTES = 15;

    @Autowired
    private BrokerCache brokerCache;

    public void runIfMarketOpen(final Runnable runnable) {
        try {
            final DateTime now = new DateTime();

            final MarketState marketState = brokerCache.getMarketState(now);
            if (this.isMarketOpenWithinOffset(marketState, 15, now)) {
                log.info("MarketState {} is within offset. Executing runnable {}", runnable);
                runnable.run();
            }
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    public Date getStartOfToday() {
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTime();
    }

    public boolean isMarketOpenWithinOffset(final MarketState marketState, final int thresholdMinutes, final DateTime now) {
        final Optional<DateTime> openTime = marketState.getExtendedOpenTime();
        final Optional<DateTime> closeTime = marketState.getExtendedCloseTime();

        if (!openTime.isPresent() || !closeTime.isPresent()) {
            return true;
        }

        final DateTime openTimeWithOffset = openTime.get().minusMinutes(thresholdMinutes);
        final DateTime closeTimeWithOffset = closeTime.get().plusMinutes(thresholdMinutes);

        return now.isAfter(openTimeWithOffset) && now.isBefore(closeTimeWithOffset);
    }

}
