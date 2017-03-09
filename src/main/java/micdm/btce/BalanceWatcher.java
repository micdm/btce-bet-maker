package micdm.btce;

import io.reactivex.Flowable;
import org.slf4j.Logger;

import java.math.BigDecimal;

class BalanceWatcher {

    private static class BalanceAndBalance {

        final BigDecimal previousBalance;
        final BigDecimal newBalance;

        BalanceAndBalance(BigDecimal previousBalance, BigDecimal newBalance) {
            this.previousBalance = previousBalance;
            this.newBalance = newBalance;
        }
    }

    private final DataProvider dataProvider;
    private final Logger logger;

    BalanceWatcher(DataProvider dataProvider, Logger logger) {
        this.dataProvider = dataProvider;
        this.logger = logger;
    }

    void init() {
        getBalanceInfo().subscribe(logger::info);
    }

    Flowable<String> getBalanceInfo() {
        return Flowable.combineLatest(
            dataProvider.getBalance().take(1),
            dataProvider.getBalance()
                .scan(new BalanceAndBalance(null, null), (accumulated, balance) ->
                    new BalanceAndBalance(accumulated.newBalance, balance)
                )
                .skip(2),
            (startBalance, bab) ->
                String.format("Balance: %s (%s from previous, %s from start)", bab.newBalance, bab.newBalance.subtract(bab.previousBalance),
                              bab.newBalance.subtract(startBalance))
        );
    }
}
