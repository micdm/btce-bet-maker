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
        getBalanceInfo().subscribe(logger::warn);
    }

    Flowable<String> getBalanceInfo() {
        return Flowable.zip(
            dataProvider.getBalance().skip(1),
            dataProvider.getBalance().compose(commonFunctions.getPrevious()),
            dataProvider.getBalance().take(1).switchMap(commonFunctions::only),
            (newBalance, previousBalance, startBalance) ->
                String.format("Balance: %s (%s from previous, %s from start)", newBalance, newBalance.subtract(previousBalance),
                              newBalance.subtract(startBalance))
        );
    }
}
