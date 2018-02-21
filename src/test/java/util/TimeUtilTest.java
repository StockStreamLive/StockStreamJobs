package util;

import com.cheddar.robinhood.data.MarketState;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class TimeUtilTest {

    private TimeUtil timeUtil;

    @Before
    public void setupTest() {
        timeUtil = new TimeUtil();
    }

    @Test
    public void testIsMarketOpenWithinOffset_noDate_expectTrue() {
        final MarketState mockMarketState = Mockito.mock(MarketState.class);
        when(mockMarketState.getExtendedOpenTime()).thenReturn(Optional.empty());
        when(mockMarketState.getExtendedCloseTime()).thenReturn(Optional.empty());

        final boolean isOpen = timeUtil.isMarketOpenWithinOffset(mockMarketState, 5, new DateTime());

        assertTrue(isOpen);
    }

    @Test
    public void testIsMarketOpenWithinOffset_beforeOpen_expectFalse() {
        final MarketState mockMarketState = Mockito.mock(MarketState.class);
        when(mockMarketState.getExtendedOpenTime()).thenReturn(Optional.of(new DateTime(2017, 5, 30, 9, 0)));
        when(mockMarketState.getExtendedCloseTime()).thenReturn(Optional.of(new DateTime(2017, 5, 31, 9, 0)));

        final boolean isOpen = timeUtil.isMarketOpenWithinOffset(mockMarketState, 5, new DateTime(2017, 5, 29, 9, 0));

        assertFalse(isOpen);
    }
}
