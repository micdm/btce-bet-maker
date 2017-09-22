package micdm.btce;

import micdm.btce.local.LocalModule;
import micdm.btce.remote.console.ConsoleModule;

public class Main {

    public static void main(String[] args) {
        if (args.length != 0 && args[0].equals("test")) {
            runTest(args[1], Integer.valueOf(args[2]));
        } else {
            runMain();
        }
    }

    private static void runTest(String pathToDataFile, int currentBetStrategy) {
        TestComponent component = DaggerTestComponent.builder()
            .localModule(new LocalModule(pathToDataFile, currentBetStrategy))
            .build();
        component.getBalanceWatcher().init();
        component.getRoundWatcher().init();
        component.getBetHandler().init();
        component.getDataProvider().init();
    }

    private static void runMain() {
        MainComponent component = DaggerMainComponent.builder()
            .consoleModule(new ConsoleModule(12001))
            .build();
        component.getBalanceWatcher().init();
        component.getRoundWatcher().init();
        component.getBetHandler().init();
        component.getMainThreadExecutor().run();
    }
}
