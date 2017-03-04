package micdm.btce;

import io.reactivex.Flowable;

import java.math.BigDecimal;

interface DataProvider {

    Flowable<Round> getRounds();
    Flowable<BigDecimal> getBalance();
}
