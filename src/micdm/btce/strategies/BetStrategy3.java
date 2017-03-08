package micdm.btce.strategies;

import micdm.btce.models.Bet;
import micdm.btce.models.ImmutableBet;
import micdm.btce.models.Round;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

// Стратегия 3: против большинства ставок
public class BetStrategy3 implements BetStrategy {

    @Override
    public Set<Bet> getBets(Round round) {
        Set<Bet> bets = new HashSet<>();
        bets.add(
            ImmutableBet.builder()
                .type(getType(round))
                .amount(new BigDecimal("0.001"))
                .build()
        );
        return bets;
    }

    private Bet.Type getType(Round round) {
        if (round.downCount() == round.upCount()) {
            return Math.random() > 0.5 ? Bet.Type.DOWN : Bet.Type.UP;
        }
        return round.downCount() > round.upCount() ? Bet.Type.UP : Bet.Type.DOWN;
    }
}
