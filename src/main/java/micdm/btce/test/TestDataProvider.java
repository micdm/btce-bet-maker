package micdm.btce.test;

import com.google.gson.Gson;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import micdm.btce.DataProvider;
import micdm.btce.models.Round;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;

class TestDataProvider implements DataProvider {

    private final BalanceBuffer balanceBuffer;
    private final Gson gson;
    private final String pathToData;

    private Flowable<Round> source;

    TestDataProvider(BalanceBuffer balanceBuffer, Gson gson, String pathToData) {
        this.balanceBuffer = balanceBuffer;
        this.gson = gson;
        this.pathToData = pathToData;
    }

    @Override
    public Flowable<Round> getRounds() {
        if (source == null) {
            source = Flowable
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
                .takeLast(280)
                .replay()
                .autoConnect();
        }
        return source;
    }

    @Override
    public Flowable<BigDecimal> getBalance() {
        return balanceBuffer.getBalance();
    }
}
