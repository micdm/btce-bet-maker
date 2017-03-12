package micdm.btce;

import io.reactivex.Flowable;

public interface SystemSettings {

    Flowable<Boolean> isBettingEnabled();
}
