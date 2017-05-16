package micdm.btce.test;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dagger.Module;
import dagger.Provides;
import io.reactivex.Flowable;
import micdm.btce.BetHandler;
import micdm.btce.BetMaker;
import micdm.btce.DataProvider;
import micdm.btce.SystemSettings;
import micdm.btce.models.Round;
import micdm.btce.strategies.BetStrategy;

import javax.inject.Singleton;
import java.util.Map;

@Module
public class TestModule {

    private final String pathToData;
    private final int currentBetStrategy;

    public TestModule(String pathToData, int currentBetStrategy) {
        this.pathToData = pathToData;
        this.currentBetStrategy = currentBetStrategy;
    }

    @Provides
    @Singleton
    Gson provideGson(RoundTypeAdapter roundTypeAdapter) {
        return new GsonBuilder()
            .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Round.class, roundTypeAdapter)
            .create();
    }

    @Provides
    @Singleton
    RoundTypeAdapter provideRoundTypeAdapter() {
        return new RoundTypeAdapter();
    }

    @Provides
    @Singleton
    DataProvider provideDataProvider(BalanceBuffer balanceBuffer, Gson gson) {
        return new TestDataProvider(balanceBuffer, gson, pathToData);
    }

    @Provides
    @Singleton
    BetHandler provideBetHandler(BalanceBuffer balanceBuffer, BetMaker betMaker, DataProvider dataProvider) {
        TestBetHandler instance = new TestBetHandler(balanceBuffer, betMaker, dataProvider);
        instance.init();
        return instance;
    }

    @Provides
    @Singleton
    BalanceBuffer provideBalanceBuffer() {
        return new BalanceBuffer();
    }

    @Provides
    @Singleton
    SystemSettings provideSystemSettings() {
        return new SystemSettings() {
            @Override
            public Flowable<Boolean> isBettingEnabled() {
                return Flowable.just(true);
            }
        };
    }

    @Provides
    @Singleton
    BetStrategy provideCurrentBetStrategy(Map<Integer, BetStrategy> strategies) {
        return strategies.get(currentBetStrategy);
    }
}
