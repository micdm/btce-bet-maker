package micdm.btce.test;

import com.google.gson.Gson;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.ReplayProcessor;
import micdm.btce.DataProvider;
import micdm.btce.models.Round;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;

class TestDataProvider implements DataProvider {

    private final BalanceBuffer balanceBuffer;
    private final Gson gson;
    private final String pathToData;

    private final FlowableProcessor<Round> rounds = ReplayProcessor.create();

    TestDataProvider(BalanceBuffer balanceBuffer, Gson gson, String pathToData) {
        this.balanceBuffer = balanceBuffer;
        this.gson = gson;
        this.pathToData = pathToData;
    }

    void init() {
        Flowable
            .create((FlowableEmitter<Round> source) -> {
                try (BufferedReader reader = new BufferedReader(new FileReader(pathToData))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        source.onNext(gson.fromJson(line, Round.class));
                    }
                    source.onComplete();
                } catch (Exception e) {
                    source.onError(e);
                }
            }, BackpressureStrategy.BUFFER)
            .subscribe(rounds);
    }

    @Override
    public Flowable<Round> getRounds() {
        return rounds;
    }

    @Override
    public Flowable<BigDecimal> getBalance() {
        return balanceBuffer.getBalance();
    }
}
