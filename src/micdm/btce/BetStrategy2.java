package micdm.btce;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// Стратегия 2: вслед за большинством ставок
class BetStrategy2 implements BetStrategy {

    private final int delta;

    BetStrategy2(int delta) {
        this.delta = delta;
    }

    @Override
    public Set<Bet> getBets(Round round) {
        Bet.Type type = getType(round);
        if (type == null) {
            return Collections.emptySet();
        }
        Set<Bet> bets = new HashSet<>();
        bets.add(
            ImmutableBet.builder()
                .type(type)
                .amount(new BigDecimal("0.002"))
                .build()
        );
        return bets;
    }

    private Bet.Type getType(Round round) {
        if (Math.abs(round.downCount() - round.upCount()) < delta) {
            return null;
        }
        return round.downCount() > round.upCount() ? Bet.Type.DOWN : Bet.Type.UP;
    }
}
