package micdm.btce.remote;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AccountIdProvider {

    private static final String DATA_URL = "https://btc-e.nz/bets";
    private static final Pattern PATTERN = Pattern.compile("_pusher.subscribe\\(\"([a-f0-9]{64})\"\\)");

    private final Authenticator authenticator;
    private final Scheduler ioScheduler;
    private final OkHttpClient okHttpClient;

    AccountIdProvider(Authenticator authenticator, Scheduler ioScheduler, OkHttpClient okHttpClient) {
        this.authenticator = authenticator;
        this.ioScheduler = ioScheduler;
        this.okHttpClient = okHttpClient;
    }

    Flowable<String> getAccountId() {
        return authenticator.run().flatMap(o ->
            Flowable
                .<String>create(source -> {
                    Response response = okHttpClient.newCall(
                        new Request.Builder()
                            .get()
                            .url(DATA_URL)
                            .build()
                    ).execute();
                    Matcher matcher = PATTERN.matcher(response.body().string());
                    if (!matcher.find()) {
                        source.onError(new IllegalStateException("cannot find account ID"));
                    } else {
                        source.onNext(matcher.group(1));
                        source.onComplete();
                    }
                }, BackpressureStrategy.BUFFER)
                .subscribeOn(ioScheduler)
        );
    }
}
