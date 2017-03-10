package micdm.btce.test;

import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;

import java.math.BigDecimal;

class BalanceBuffer {

    private final FlowableProcessor<BigDecimal> deltas = PublishProcessor.create();

    Flowable<BigDecimal> getBalance() {
        return deltas.scan(BigDecimal.ZERO, BigDecimal::add);
    }

    void changeBalance(BigDecimal delta) {
        deltas.onNext(delta);
    }
}
