package lambda;

import com.cheddar.robinhood.data.MarginBalances;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PortfolioSynchronizerTest {

    private PortfolioSynchronizer portfolioSynchronizer;

    @Before
    public void setupTest() {
        portfolioSynchronizer = new PortfolioSynchronizer();
    }

    @Test
    public void testComputeCashBalance_lessThanZero_expectZero() {
        final double balance = portfolioSynchronizer.computeCashBalance(new MarginBalances(500, 1000, 0));
        assertEquals(balance, 0, .001);
    }

    @Test
    public void testComputeCashBalance_2kTotal1kMargin_expect1k() {
        final double balance = portfolioSynchronizer.computeCashBalance(new MarginBalances(2000, 1000, 0));
        assertEquals(balance, 1000, .001);
    }

    @org.junit.Test
    public void testComputeMarginSpent_2kTotal1kMargin_expectZero() {
        final double spentMargin = portfolioSynchronizer.computeSpentMargin(new MarginBalances(2000, 1000, 0));
        assertEquals(spentMargin, 0, .001);
    }

    @Test
    public void testComputeMarginSpent_1kTotal1kMargin_expectZero() {
        final double spentMargin = portfolioSynchronizer.computeSpentMargin(new MarginBalances(2000, 1000, 0));
        assertEquals(spentMargin, 0, .001);
    }

    @Test
    public void testComputeMarginSpent_1kTotal2kMargin_expect1k() {
        final double spentMargin = portfolioSynchronizer.computeSpentMargin(new MarginBalances(1000, 2000, 0));
        assertEquals(spentMargin, 1000, .001);
    }

}
