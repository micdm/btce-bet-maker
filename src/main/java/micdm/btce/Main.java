package micdm.btce;

import micdm.btce.test.TestModule;

public class Main {

    public static void main(String[] args) {
        if (args.length != 0 && args[0].equals("test")) {
            runTest();
        } else {
            runMain();
        }
    }

    private static void runTest() {
        TestComponent component = DaggerTestComponent.builder()
            .testModule(new TestModule("/home/mic/dev/loto-tools/btce/data/merged.data"))
            .build();
        component.getBalanceWatcher();
        component.getBetHandler();
        component.getRoundWatcher();
    }

    private static void runMain() {
        MainComponent component = DaggerMainComponent.builder().build();
        component.getBalanceWatcher();
        component.getBetHandler();
        component.getRoundWatcher();
        component.getMainThreadExecutor().run();
    }
}
