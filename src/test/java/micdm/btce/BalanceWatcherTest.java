package micdm.btce;

import io.reactivex.Flowable;
import micdm.btce.misc.CommonFunctions;
import micdm.btce.models.Round;
import micdm.btce.remote.RemoteDataProvider;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BalanceWatcherTest {

    public BalanceWatcherTest() {

    }

    @Test
    public void testGetBalanceInfo() {
        RemoteDataProvider dataProvider = mock(RemoteDataProvider.class);
        when(dataProvider.getBalance()).thenReturn(
            Flowable.just(
                new BigDecimal(1),
                new BigDecimal(3),
                new BigDecimal(7)
            )
        );
        Round round = mock(Round.class);
        when(round.number()).thenReturn(1);
        when(dataProvider.getRounds()).thenReturn(
            Flowable.just(round)
        );
        BalanceWatcher balanceWatcher = new BalanceWatcher(new CommonFunctions(), dataProvider, LoggerFactory.getLogger("local"));
        balanceWatcher.getBalanceInfo()
            .test()
            .assertValues(
                "Round: 1, balance: 3 (2 from previous, 2 from start)",
                "Round: 1, balance: 7 (4 from previous, 6 from start)"
            );
    }
}
