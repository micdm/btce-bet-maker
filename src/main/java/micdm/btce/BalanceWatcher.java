package micdm.btce;

import io.reactivex.Flowable;
import micdm.btce.misc.CommonFunctions;
import org.slf4j.Logger;

class BalanceWatcher {

    private final CommonFunctions commonFunctions;
    private final DataProvider dataProvider;
    private final Logger logger;

    BalanceWatcher(CommonFunctions commonFunctions, DataProvider dataProvider, Logger logger) {
        this.commonFunctions = commonFunctions;
        this.dataProvider = dataProvider;
        this.logger = logger;
    }

    void init() {
        getBalanceInfo().subscribe(logger::info);
    }

    Flowable<String> getBalanceInfo() {
        return Flowable
            .zip(
                dataProvider.getBalance().skip(1),
                dataProvider.getBalance().compose(commonFunctions.getPrevious()),
                dataProvider.getBalance().take(1).switchMap(commonFunctions::only),
                (newBalance, previousBalance, startBalance) ->
                    String.format("balance: %s (%s from previous, %s from start)", newBalance, newBalance.subtract(previousBalance),
                                  newBalance.subtract(startBalance))
            )
            .withLatestFrom(
                dataProvider.getRounds(),
                (output, round) ->
                    String.format("Round: %s, %s", round.number(), output)
            );
    }
}
