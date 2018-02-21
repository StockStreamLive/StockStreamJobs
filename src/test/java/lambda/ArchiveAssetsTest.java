package lambda;

import cache.BrokerCache;
import com.cheddar.robinhood.client.RobinhoodClient;
import com.cheddar.robinhood.data.Position;
import com.cheddar.robinhood.data.Quote;
import com.cheddar.robinhood.exception.RobinhoodException;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import stockstream.database.Asset;
import stockstream.database.AssetRegistry;
import stockstream.database.InstrumentRegistry;
import stockstream.database.InstrumentStub;
import stockstream.logic.Scheduler;
import util.TimeUtil;

import java.util.Collection;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class ArchiveAssetsTest {

    @Mock
    private Scheduler scheduler;

    @Mock
    private BrokerCache brokerCache;

    @Mock
    private TimeUtil timeUtil;

    @Mock
    private RobinhoodClient robinhoodClient;

    @Mock
    private AssetRegistry assetRegistry;

    @Mock
    private InstrumentRegistry instrumentRegistry;

    @InjectMocks
    private ArchiveAssets archiveAssets;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAllAssets_onePosition_expectOneAssetReturned() throws RobinhoodException {
        final Quote quote = new Quote("AMZN", 2.50, 2.45, 10, 10, 2.49, 0, 2.0d, 2.0d, "http://instrument");

        final InstrumentStub instrument = new InstrumentStub();
        instrument.setUrl("http://instrument");
        instrument.setSymbol("AMZN");

        when(robinhoodClient.getPositions()).thenReturn(ImmutableList.of(new Position(1, 900, "http://instrument")));
        when(brokerCache.getInstrumentForURL(any())).thenReturn(Optional.of(instrument));
        when(robinhoodClient.getQuotes(any())).thenReturn(ImmutableList.of(quote));

        final Collection<Asset> assets = archiveAssets.getAllAssets();

        assertEquals(1, assets.size());
        assertEquals("AMZN", assets.iterator().next().getSymbol());
    }


}
