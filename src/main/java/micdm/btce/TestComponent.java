package micdm.btce;

import dagger.Component;
import micdm.btce.misc.MiscModule;
import micdm.btce.strategies.StrategyModule;
import micdm.btce.test.TestModule;

import javax.inject.Singleton;

@Singleton
@Component(modules = {MainModule.class, MiscModule.class, StrategyModule.class, TestModule.class})
interface TestComponent {

    BalanceWatcher getBalanceWatcher();
    BetHandler getBetHandler();
    RoundWatcher getRoundWatcher();
}
