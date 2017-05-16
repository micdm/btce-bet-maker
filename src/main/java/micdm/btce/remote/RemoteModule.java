package micdm.btce.remote;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.neovisionaries.ws.client.WebSocketFactory;
import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import micdm.btce.*;
import micdm.btce.misc.CommonFunctions;
import micdm.btce.remote.console.ConsoleModule;
import micdm.btce.remote.console.UserConsole;
import micdm.btce.strategies.BetStrategy;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.slf4j.Logger;

import javax.inject.Named;
import javax.inject.Singleton;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Module(includes = {ConsoleModule.class})
public class RemoteModule {

    @Provides
    @Singleton
    @Named("remote")
    OkHttpClient provideOkHttpClient(CookieStore cookieStore, Logger logger) {
        return new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                long now = System.currentTimeMillis();
                Response response = chain.proceed(chain.request());
                logger.debug("{} to {} in {}ms", chain.request(), response, System.currentTimeMillis() - now);
                return response;
            })
            .cookieJar(cookieStore)
            .followRedirects(false)
            .build();
    }

    @Provides
    @Singleton
    WebSocketFactory provideWebSocketFactory() {
        return new WebSocketFactory();
    }

    @Provides
    @Singleton
    CookieStore provideCookieStore(Gson gson, Logger logger) {
        return new CookieStore(gson, logger);
    }

    @Provides
    @Singleton
    Gson provideGson() {
        return new GsonBuilder()
            .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    }

    @Provides
    @Singleton
    MessageDigest provideMessageDigest() {
        try {
            return MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 digest not available");
        }
    }

    @Provides
    @Singleton
    AccountIdProvider provideAccountIdProvider(Authenticator authenticator, @Named("io") Scheduler ioScheduler, @Named("remote") OkHttpClient okHttpClient) {
        return new AccountIdProvider(authenticator, ioScheduler, okHttpClient);
    }

    @Provides
    @Singleton
    Authenticator provideAuthenticator(Config config, Gson gson, @Named("io") Scheduler ioScheduler, Logger logger, MessageDigest messageDigest, @Named("remote") OkHttpClient okHttpClient) {
        return new Authenticator(config, gson, ioScheduler, logger, messageDigest, okHttpClient);
    }

    @Provides
    @Singleton
    CsrfTokenProvider provideCsrfTokenProvider(Authenticator authenticator, @Named("io") Scheduler ioScheduler, @Named("remote") OkHttpClient okHttpClient) {
        return new CsrfTokenProvider(authenticator, ioScheduler, okHttpClient);
    }

    @Provides
    @Singleton
    BetHandler provideBetHandler(Authenticator authenticator, BetMaker betMaker, CsrfTokenProvider csrfTokenProvider, Gson gson, Logger logger, @Named("remote") OkHttpClient okHttpClient) {
        RemoteBetHandler instance = new RemoteBetHandler(authenticator, betMaker, csrfTokenProvider, gson, logger, okHttpClient);
        instance.init();
        return instance;
    }

    @Provides
    @Singleton
    DataProvider provideDataProvider(AccountIdProvider accountIdProvider, Gson gson, @Named("io") Scheduler ioScheduler, Logger logger, WebSocketFactory webSocketFactory) {
        return new RemoteDataProvider(accountIdProvider, gson, ioScheduler, logger, webSocketFactory);
    }

    @Provides
    @Singleton
    SystemSettings provideSystemSettings(CommonFunctions commonFunctions, UserConsole userConsole) {
        return new RemoteSystemSettings(commonFunctions, userConsole);
    }

    @Provides
    @Singleton
    BetStrategy provideCurrentBetStrategy(Map<Integer, BetStrategy> strategies) {
        return strategies.get(2);
    }
}
