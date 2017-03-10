package micdm.btce;

import dagger.Component;
import micdm.btce.misc.MainThreadExecutor;
import micdm.btce.misc.MiscModule;
import micdm.btce.remote.RemoteModule;
import micdm.btce.strategies.StrategyModule;

import javax.inject.Singleton;

@Singleton
@Component(modules = {MainModule.class, MiscModule.class, RemoteModule.class, StrategyModule.class})
interface MainComponent {

    BalanceWatcher getBalanceWatcher();
    BetHandler getBetHandler();
    RoundWatcher getRoundWatcher();
    MainThreadExecutor getMainThreadExecutor();
}
