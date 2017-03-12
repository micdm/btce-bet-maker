package micdm.btce.remote.console;

public enum UserCommand {

    ENABLE_BETTING("betting", "enable"),
    DISABLE_BETTING("betting", "disable");

    private final String category;
    private final String action;

    UserCommand(String category, String action) {
        this.category = category;
        this.action = action;
    }

    boolean is(String category, String action) {
        return this.category.equals(category) && this.action.equals(action);
    }
}
