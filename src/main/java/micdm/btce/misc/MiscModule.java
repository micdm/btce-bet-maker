package micdm.btce.misc;

import dagger.Module;
import dagger.Provides;
import org.slf4j.Logger;

import javax.inject.Singleton;

@Module
public class MiscModule {

    @Provides
    @Singleton
    MainThreadExecutor provideMainThreadExecutor(Logger logger) {
        return new MainThreadExecutor(logger);
    }

    @Provides
    @Singleton
    CommonFunctions provideCommonFunctions() {
        return new CommonFunctions();
    }
}
