package micdm.btce;

import micdm.btce.remote.console.ConsoleModule;
import micdm.btce.test.TestModule;

public class Main {

    public static void main(String[] args) {
        if (args.length != 0 && args[0].equals("test")) {
            runTest(args[1]);
        } else {
            runMain();
        }
    }

    private static void runTest(String pathToDataFile) {
        TestComponent component = DaggerTestComponent.builder()
            .testModule(new TestModule(pathToDataFile))
            .build();
        component.getBalanceWatcher();
        component.getBetHandler();
        component.getRoundWatcher();
    }

    private static void runMain() {
        MainComponent component = DaggerMainComponent.builder()
            .consoleModule(new ConsoleModule(12000))
            .build();
        component.getBalanceWatcher();
        component.getBetHandler();
        component.getRoundWatcher();
        component.getMainThreadExecutor().run();
    }
}
