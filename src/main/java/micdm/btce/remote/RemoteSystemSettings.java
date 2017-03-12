package micdm.btce.remote;

import io.reactivex.Flowable;
import micdm.btce.SystemSettings;
import micdm.btce.remote.console.UserCommand;
import micdm.btce.remote.console.UserConsole;
import micdm.btce.misc.CommonFunctions;

class RemoteSystemSettings implements SystemSettings {

    private final CommonFunctions commonFunctions;
    private final UserConsole userConsole;

    RemoteSystemSettings(CommonFunctions commonFunctions, UserConsole userConsole) {
        this.commonFunctions = commonFunctions;
        this.userConsole = userConsole;
    }

    @Override
    public Flowable<Boolean> isBettingEnabled() {
        return Flowable
            .merge(
                userConsole.getCommands()
                    .filter(commonFunctions.isEqual(UserCommand.ENABLE_BETTING))
                    .map(commonFunctions::toTrue),
                userConsole.getCommands()
                    .filter(commonFunctions.isEqual(UserCommand.DISABLE_BETTING))
                    .map(commonFunctions::toFalse)
            )
            .startWith(true);
    }
}
