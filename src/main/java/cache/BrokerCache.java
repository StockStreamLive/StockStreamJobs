package cache;

import com.cheddar.robinhood.client.RobinhoodClient;
import com.cheddar.robinhood.data.MarketState;
import com.cheddar.robinhood.exception.RobinhoodException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.database.InstrumentRegistry;
import stockstream.database.InstrumentStub;

import java.util.Optional;

@Slf4j
public class BrokerCache {

    // Day of year -> DayObject
    private LoadingCache<Integer, MarketState> marketStateCache =
            CacheBuilder.newBuilder().build(new CacheLoader<Integer, MarketState>() {
                @Override
                public MarketState load(final Integer key) throws Exception {
                    DateTime thisDay = new DateTime(0).withYear(new DateTime().getYear()).withDayOfYear(key);
                    MarketState day = robinhoodClient.getMarketStateForDate(thisDay);
                    return day;
                }
            });

    private LoadingCache<String, InstrumentStub> instrumentUrlToInstrument =
            CacheBuilder.newBuilder().build(new CacheLoader<String, InstrumentStub>() {
                @Override
                public InstrumentStub load(final String url) throws Exception {
                    final Optional<InstrumentStub> instrumentOptional = instrumentRegistry.getInstrumentByURL(url);
                    if (!instrumentOptional.isPresent()) {
                        throw new RobinhoodException(String.format("No instrument for URL %s", url));
                    }
                    return instrumentOptional.get();
                }
            });

    @Autowired
    private RobinhoodClient robinhoodClient;

    @Autowired
    private InstrumentRegistry instrumentRegistry;

    public MarketState getMarketState(final DateTime forDate) {
        try {
            return marketStateCache.get(forDate.getDayOfYear());
        } catch (final Exception ex) {
            log.warn(ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    public Optional<InstrumentStub> getInstrumentForURL(final String url) {
        try {
            return Optional.ofNullable(instrumentUrlToInstrument.get(url));
        } catch (final Exception ex) {
            log.warn(ex.getMessage(), ex);
        }
        return Optional.empty();
    }



}
