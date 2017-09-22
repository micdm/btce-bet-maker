package micdm.btce;

import dagger.Component;
import micdm.btce.local.LocalModule;
import micdm.btce.misc.MiscModule;
import micdm.btce.strategies.StrategyModule;

import javax.inject.Singleton;

@Singleton
@Component(modules = {MainModule.class, MiscModule.class, StrategyModule.class, LocalModule.class})
interface TestComponent {

    BalanceWatcher getBalanceWatcher();
    BetHandler getBetHandler();
    DataProvider getDataProvider();
    RoundWatcher getRoundWatcher();
}
