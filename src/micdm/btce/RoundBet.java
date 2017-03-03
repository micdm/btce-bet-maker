package micdm.btce;

import org.immutables.value.Value;

import java.util.Set;

@Value.Immutable
interface RoundBet {

    int number();
    Set<Bet> downBets();
    Set<Bet> upBets();
}
