package micdm.btce.strategies;

import micdm.btce.models.Bet;
import micdm.btce.models.ImmutableBet;
import micdm.btce.models.Round;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

// Стратегия 1: чистая случайность
class BetStrategy1 implements BetStrategy {

    @Override
    public Set<Bet> getBets(Round round) {
        Set<Bet> bets = new HashSet<>();
        bets.add(
            ImmutableBet.builder()
                .type(Math.random() > 0.5 ? Bet.Type.DOWN : Bet.Type.UP)
                .amount(new BigDecimal("0.001"))
                .build()
        );
        return bets;
    }
}
