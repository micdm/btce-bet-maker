package micdm.btce;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.neovisionaries.ws.client.WebSocketFactory;
import io.reactivex.schedulers.Schedulers;
import micdm.btce.misc.CookieStore;
import micdm.btce.misc.MainThreadExecutor;
import micdm.btce.strategies.BetStrategy2;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Main {

    public static void main(String[] args) {
        Gson gson = new GsonBuilder()
            .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

        Logger logger = LoggerFactory.getLogger("main");

        OkHttpClient httpClient = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                long now = System.currentTimeMillis();
                Response response = chain.proceed(chain.request());
                logger.debug("{} to {} in {}ms", chain.request(), response, System.currentTimeMillis() - now);
                return response;
            })
            .cookieJar(new CookieStore(gson, logger))
            .followRedirects(false)
            .build();

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException();
        }

        Authenticator authenticator = new Authenticator(gson, logger, digest, httpClient, Schedulers.io());

        AccountIdProvider accountIdProvider = new AccountIdProvider(authenticator, Schedulers.io(), httpClient);

        DataProvider dataProvider = new RemoteDataProvider(accountIdProvider, gson, logger, Schedulers.io(), new WebSocketFactory());

        BetMaker betMaker = new BetMaker(dataProvider, new BetStrategy2(logger));

        CsrfTokenProvider csrfTokenProvider = new CsrfTokenProvider(authenticator, Schedulers.io(), httpClient);

        RemoteBetHandler betHandler = new RemoteBetHandler(authenticator, betMaker, csrfTokenProvider, gson, logger, httpClient);
        betHandler.init();

        BalanceWatcher balanceWatcher = new BalanceWatcher(dataProvider, logger);
        balanceWatcher.init();

        RoundWatcher roundWatcher = new RoundWatcher(dataProvider, logger);
        roundWatcher.init();

        MainThreadExecutor executor = new MainThreadExecutor(logger);
        executor.run();
    }
}
