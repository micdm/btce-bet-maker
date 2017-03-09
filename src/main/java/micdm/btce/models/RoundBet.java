package micdm.btce.models;

import org.immutables.value.Value;

import java.util.Set;

@Value.Immutable
public interface RoundBet {

    // TODO: number?
    int number();
    Set<Bet> downBets();
    Set<Bet> upBets();
}
