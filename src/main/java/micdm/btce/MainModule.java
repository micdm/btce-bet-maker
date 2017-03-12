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
        BalanceWatcher instance = new BalanceWatcher(commonFunctions, dataProvider, logger);
        instance.init();
        return instance;
    }

    @Provides
    @Singleton
    RoundWatcher provideRoundWatcher(DataProvider dataProvider, Logger logger) {
        RoundWatcher instance = new RoundWatcher(dataProvider, logger);
        instance.init();
        return instance;
    }

    @Provides
    @Singleton
    BetMaker provideBetMaker(BetStrategy betStrategy, DataProvider dataProvider, SystemSettings systemSettings) {
        return new BetMaker(betStrategy, dataProvider, systemSettings);
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
