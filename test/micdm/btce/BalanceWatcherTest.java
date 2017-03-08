package micdm.btce;

import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BalanceWatcherTest {

    @Test
    void testGetBalanceInfo() {
        RemoteDataProvider dataProvider = mock(RemoteDataProvider.class);
        when(dataProvider.getBalance()).thenReturn(
            Flowable.just(
                new BigDecimal(1),
                new BigDecimal(3),
                new BigDecimal(7)
            )
        );
        BalanceWatcher balanceWatcher = new BalanceWatcher(dataProvider, LoggerFactory.getLogger("test"));
        balanceWatcher.getBalanceInfo().test().assertResult(
            "Balance: 3 (2 from previous, 2 from start)",
            "Balance: 7 (4 from previous, 6 from start)"
        );
    }
}
