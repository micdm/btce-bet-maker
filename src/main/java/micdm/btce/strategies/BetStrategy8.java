package micdm.btce.strategies;

import io.reactivex.Maybe;
import micdm.btce.models.Bet;
import micdm.btce.models.ImmutableBet;
import micdm.btce.models.Round;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

// Стратегия 8: всегда на UP
class BetStrategy8 implements BetStrategy {

    @Override
    public Maybe<Collection<Bet>> getBets(Round round) {
        return Maybe.just(Collections.singleton(
            ImmutableBet.builder()
                .type(Bet.Type.UP)
                .amount(new BigDecimal("0.001"))
                .build()
        ));
    }
}
