package micdm.btce.models;

import org.immutables.value.Value;

import java.util.Collection;
import java.util.stream.Collectors;

@Value.Immutable
public abstract class RoundBet {

    public abstract int number();
    public abstract Collection<Bet> bets();

    public Collection<Bet> downBets() {
        return bets().stream()
            .filter(bet -> bet.type() == Bet.Type.DOWN)
            .collect(Collectors.toList());
    }

    public Collection<Bet> upBets() {
        return bets().stream()
            .filter(bet -> bet.type() == Bet.Type.UP)
            .collect(Collectors.toList());
    }
}
