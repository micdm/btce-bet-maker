package micdm.btce.remote;

import com.google.gson.Gson;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import micdm.btce.Config;
import micdm.btce.misc.Irrelevant;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;

class Authenticator {

    private static class LoginResult {

        static class Data {

            static class Work {

                final double target;
                final String data;

                Work(double target, String data) {
                    this.target = target;
                    this.data = data;
                }
            }

            final int PoW;
            final Work work;

            Data(int poW, Work work) {
                PoW = poW;
                this.work = work;
            }
        }

        final int success;
        final String error;
        final Data data;

        LoginResult(int success, String error, Data data) {
            this.success = success;
            this.error = error;
            this.data = data;
        }
    }

    private static final String LOGIN_URL = "https://btc-e.nz/ajax/login";
    private static final String LOGIN_FIELD = "email";
    private static final String PASSWORD_FIELD = "password";
    private static final String NONCE_FIELD = "PoW_nonce";

    private final Config config;
    private final Gson gson;
    private final Scheduler ioScheduler;
    private final Logger logger;
    private final MessageDigest messageDigest;
    private final OkHttpClient okHttpClient;

    private Flowable<Object> cached;

    Authenticator(Config config, Gson gson, Scheduler ioScheduler, Logger logger, MessageDigest messageDigest, OkHttpClient okHttpClient) {
        this.config = config;
        this.gson = gson;
        this.ioScheduler = ioScheduler;
        this.logger = logger;
        this.messageDigest = messageDigest;
        this.okHttpClient = okHttpClient;
    }

    Flowable<Object> run() {
        if (cached == null) {
            logger.info("Logging in...");
            cached = getNonce()
                .flatMap(this::loginWithNonce)
                .share();
        }
        return cached;
    }

    private Flowable<Integer> getNonce() {
        return Flowable
            .<Integer>create(source -> {
                logger.debug("Loading nonce data...");
                Response response = okHttpClient.newCall(
                    new Request.Builder()
                        .post(
                            new FormBody.Builder()
                                .add(LOGIN_FIELD, config.LOGIN)
                                .add(PASSWORD_FIELD, config.PASSWORD)
                                .build()
                        )
                        .url(LOGIN_URL)
                        .build()
                ).execute();
                LoginResult result = gson.fromJson(response.body().string(), LoginResult.class);
                if (result.success == 0) {
                    source.onError(new IllegalStateException(String.format("success must be equal 1 (%s)", result.error)));
                } else {
                    source.onNext(getNonce(result.data.work.data, result.data.work.target));
                    source.onComplete();
                }
            }, BackpressureStrategy.BUFFER)
            .subscribeOn(ioScheduler);
    }

    private int getNonce(String data, double target) {
        int result = 0;
        BigInteger hash;
        BigInteger converted = new BigDecimal(target).toBigInteger();
        do {
            hash = new BigInteger(shuffle(getMd5AsHexString(getMd5AsHexString(data + result))), 16);
            result += 1;
        } while (hash.compareTo(converted) >= 0);
        return result;
    }

    private String getMd5AsHexString(String input) {
        String hex = new BigInteger(1, messageDigest.digest(input.getBytes())).toString(16);
        StringBuilder builder = new StringBuilder(hex).reverse();
        while (builder.length() < 32) {
            builder.insert(0, "0");
        }
        return builder.toString();
    }

    private String shuffle(String input) {
        return input.substring(16, 24) + input.substring(0, 8) + input.substring(8, 16) + input.substring(24, 32);
    }

    private Flowable<Object> loginWithNonce(int nonce) {
        return Flowable
            .create(source -> {
                logger.debug("Logging in with nonce {}...", nonce);
                Response response = okHttpClient.newCall(
                    new Request.Builder()
                        .post(
                            new FormBody.Builder()
                                .add(LOGIN_FIELD, config.LOGIN)
                                .add(PASSWORD_FIELD, config.PASSWORD)
                                .add(NONCE_FIELD, String.valueOf(nonce))
                                .build()
                        )
                        .url(LOGIN_URL)
                        .build()
                ).execute();
                LoginResult result = gson.fromJson(response.body().string(), LoginResult.class);
                if (result.success == 0) {
                    source.onError(new IllegalStateException(String.format("success must be equal 1 (%s)", result.error)));
                } else {
                    logger.info("Successfully logged in!");
                    source.onNext(Irrelevant.INSTANCE);
                    source.onComplete();
                }
            }, BackpressureStrategy.BUFFER)
            .subscribeOn(ioScheduler);
    }
}
