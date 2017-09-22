package micdm.btce;

import io.reactivex.Flowable;
import micdm.btce.models.Round;

import java.math.BigDecimal;

public interface DataProvider {

    void init();
    Flowable<Round> getRounds();
    Flowable<BigDecimal> getBalance();
}
