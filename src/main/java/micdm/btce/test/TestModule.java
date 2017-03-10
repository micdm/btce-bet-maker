package micdm.btce.test;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dagger.Module;
import dagger.Provides;
import micdm.btce.BetHandler;
import micdm.btce.BetMaker;
import micdm.btce.DataProvider;
import micdm.btce.models.Round;

import javax.inject.Singleton;

@Module
public class TestModule {

    private final String pathToData;

    public TestModule(String pathToData) {
        this.pathToData = pathToData;
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
}
