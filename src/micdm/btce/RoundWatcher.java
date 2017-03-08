package micdm.btce;

import micdm.btce.models.Round;
import org.joda.time.Duration;
import org.slf4j.Logger;

class RoundWatcher {

    private final DataProvider dataProvider;
    private final Logger logger;

    RoundWatcher(DataProvider dataProvider, Logger logger) {
        this.dataProvider = dataProvider;
        this.logger = logger;
    }

    void init() {
        dataProvider.getRounds()
            .filter(round -> round.endsIn().getStandardSeconds() % 30 == 0 || round.endsIn().isShorterThan(Duration.standardSeconds(5)))
            .distinctUntilChanged(Round::endsIn)
            .subscribe(round ->
                logger.info("Round: {}", round)
            );
    }
}
