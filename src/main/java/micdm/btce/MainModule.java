package micdm.btce;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import micdm.btce.misc.CommonFunctions;
import micdm.btce.strategies.BetStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
class MainModule {

    @Provides
    @Singleton
    Logger provideLogger() {
        return LoggerFactory.getLogger("main");
    }

    @Provides
    @Singleton
    BalanceWatcher provideBalanceWatcher(CommonFunctions commonFunctions, DataProvider dataProvider, Logger logger) {
        return new BalanceWatcher(commonFunctions, dataProvider, logger);
    }

    @Provides
    @Singleton
    RoundWatcher provideRoundWatcher(DataProvider dataProvider, Logger logger) {
        return new RoundWatcher(dataProvider, logger);
    }

    @Provides
    @Singleton
    BetMaker provideBetMaker(BetStrategy betStrategy, CommonFunctions commonFunctions, DataProvider dataProvider, SystemSettings systemSettings) {
        return new BetMaker(betStrategy, commonFunctions, dataProvider, systemSettings);
    }

    @Provides
    @Singleton
    Config provideConfig() {
        return new Config();
    }

    @Provides
    @Named("io")
    @Singleton
    Scheduler provideIoScheduler() {
        return Schedulers.io();
    }

    @Provides
    @Named("newThread")
    @Singleton
    Scheduler provideNewThreadScheduler() {
        return Schedulers.newThread();
    }
}
