package micdm.btce.strategies;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntKey;
import dagger.multibindings.IntoMap;
import io.reactivex.Scheduler;
import micdm.btce.Config;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class StrategyModule {

    @Provides
    @Singleton
    @IntoMap
    @IntKey(1)
    BetStrategy provideBetStrategy1() {
        return new BetStrategy1();
    }

    @Provides
    @Singleton
    @IntoMap
    @IntKey(2)
    BetStrategy provideBetStrategy2(Config config, Logger logger) {
        return new BetStrategy2(config, config.strategy2Config, logger);
    }

    @Provides
    @Singleton
    @IntoMap
    @IntKey(3)
    BetStrategy provideBetStrategy3() {
        return new BetStrategy3();
    }

    @Provides
    @Singleton
    @IntoMap
    @IntKey(4)
    BetStrategy provideBetStrategy4() {
        return new BetStrategy4();
    }

    @Provides
    @Singleton
    @IntoMap
    @IntKey(5)
    BetStrategy provideBetStrategy5() {
        return new BetStrategy5();
    }

    @Provides
    @Singleton
    @IntoMap
    @IntKey(6)
    BetStrategy provideBetStrategy6(Config config, Logger logger, NeuralNetworkConnector neuralNetworkConnector) {
        return new BetStrategy6(config, config.strategy6Config, logger, neuralNetworkConnector);
    }

    @Provides
    @Singleton
    @IntoMap
    @IntKey(7)
    BetStrategy provideBetStrategy7(Logger logger) {
        return new BetStrategy7(logger);
    }

    @Provides
    @Singleton
    NeuralNetworkConnector provideNeuralNetworkConnector(Config config, Logger logger, @Named("neuralNetwork") OkHttpClient okHttpClient,
                                                         @Named("neuralNetwork") Request.Builder requestBuilder, @Named("io") Scheduler ioScheduler) {
        return new NeuralNetworkConnector(config, logger, okHttpClient, requestBuilder, ioScheduler);
    }

    @Provides
    @Singleton
    @Named("neuralNetwork")
    OkHttpClient provideOkHttpClient(Logger logger) {
        return new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                long now = System.currentTimeMillis();
                Response response = chain.proceed(chain.request());
                logger.debug("{} to {} in {}ms", chain.request(), response, System.currentTimeMillis() - now);
                return response;
            })
            .build();
    }

    @Provides
    @Singleton
    @Named("neuralNetwork")
    Request.Builder provideRequestBuilder() {
        return new Request.Builder().get();
    }
}
