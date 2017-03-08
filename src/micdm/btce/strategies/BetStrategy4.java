package micdm.btce.strategies;

import micdm.btce.models.Bet;
import micdm.btce.models.ImmutableBet;
import micdm.btce.models.Round;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

// Стратегия 4: вслед за большинством по деньгам
public class BetStrategy4 implements BetStrategy {

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
        int result = round.downAmount().compareTo(round.upAmount());
        if (result == -1) {
            return Bet.Type.UP;
        }
        if (result == 1) {
            return Bet.Type.DOWN;
        }
        return Math.random() > 0.5 ? Bet.Type.DOWN : Bet.Type.UP;
    }
}
