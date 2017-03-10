package micdm.btce.strategies;

import dagger.Module;
import dagger.Provides;
import micdm.btce.Config;
import org.slf4j.Logger;

import javax.inject.Singleton;

@Module
public class StrategyModule {

    @Provides
    @Singleton
    BetStrategy provideBetStrategy(Config config, Logger logger) {
        return new BetStrategy2(config, logger);
    }
}
