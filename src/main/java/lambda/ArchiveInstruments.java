package lambda;

import com.cheddar.robinhood.client.RobinhoodClient;
import com.cheddar.robinhood.data.Instrument;
import com.cheddar.robinhood.exception.RobinhoodException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.database.InstrumentRegistry;
import stockstream.database.InstrumentStub;
import stockstream.logic.Scheduler;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class ArchiveInstruments {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private RobinhoodClient robinhoodClient;

    @Autowired
    private InstrumentRegistry instrumentRegistry;

    @PostConstruct
    public void init() {
        scheduler.scheduleJob(this::refreshInstruments, 0,  12, TimeUnit.HOURS);
    }

    private synchronized void refreshInstruments() {
        try {
            synchronizeInstruments();
        } catch (RobinhoodException e) {
            log.warn(e.getMessage(), e);
        }
    }

    private synchronized void synchronizeInstruments() throws RobinhoodException {
        final Set<Instrument> instruments = robinhoodClient.getAllInstruments();

        final List<InstrumentStub> instrumentStubs = instruments.stream().map(InstrumentStub::new).collect(Collectors.toList());

        instrumentRegistry.saveInstrumentStubs(instrumentStubs);
    }

}
