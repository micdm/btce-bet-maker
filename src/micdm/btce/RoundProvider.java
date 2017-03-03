package micdm.btce;

import io.reactivex.Flowable;

interface RoundProvider {

    Flowable<Round> getRounds();
}
