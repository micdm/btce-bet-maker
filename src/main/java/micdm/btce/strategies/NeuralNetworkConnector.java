package micdm.btce.strategies;

import io.reactivex.Single;
import micdm.btce.Config;
import okhttp3.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;

class NeuralNetworkConnector {

    static class Probabilities {

        final float decrease;
        final float increase;

        Probabilities(float decrease, float increase) {
            this.decrease = decrease;
            this.increase = increase;
        }
    }

    private final Config config;
    private final Logger logger;
    private final OkHttpClient okHttpClient;
    private final Request.Builder requestBuilder;

    NeuralNetworkConnector(Config config, Logger logger, OkHttpClient okHttpClient, Request.Builder requestBuilder) {
        this.config = config;
        this.logger = logger;
        this.okHttpClient = okHttpClient;
        this.requestBuilder = requestBuilder;
    }

    Single<Probabilities> getProbabilities(int downCount, BigDecimal downAmount, int upCount, BigDecimal upAmount,
                                                  int dayOfWeek, int minuteOfDay) {
        return Single.create(source -> {
            Call call = okHttpClient.newCall(
                requestBuilder
                    .url(String.format("%s/predict/%s,%s,%s,%s,%s,%s", config.NEURAL_NETWORK_URL, downAmount, upAmount,
                        downCount, upCount, dayOfWeek, minuteOfDay))
                    .build()
            );
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    logger.warn("Cannot connect to neural network", e);
                    source.onError(e);
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String[] parts = response.body().string().split("\n");
                    logger.debug("Probabilities are {} for decrease and {} for increase", parts[0], parts[1]);
                    source.onSuccess(new Probabilities(Float.valueOf(parts[0]), Float.valueOf(parts[1])));
                }
            });
            source.setCancellable(call::cancel);
        });
    }
}
