package micdm.btce;

import io.reactivex.Flowable;
import micdm.btce.models.Round;

import java.math.BigDecimal;

public interface DataProvider {

    Flowable<Round> getRounds();
    Flowable<BigDecimal> getBalance();
}
