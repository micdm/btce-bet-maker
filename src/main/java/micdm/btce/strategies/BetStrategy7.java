package micdm.btce.strategies;

import io.reactivex.Maybe;
import micdm.btce.models.Bet;
import micdm.btce.models.ImmutableBet;
import micdm.btce.models.Round;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashSet;

// Стратегия 7: ставим только если коэффициент выигрыша больше единицы
class BetStrategy7 implements BetStrategy {

    private static final MathContext RATIO_CONTEXT = new MathContext(6, RoundingMode.DOWN);

    private final Logger logger;

    BetStrategy7(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Maybe<Collection<Bet>> getBets(Round round) {
        Bet.Type betType = getBetType(round);
        if (betType == null) {
            return Maybe.empty();
        }
        Collection<Bet> bets = new HashSet<>();
        bets.add(
            ImmutableBet.builder()
                .type(betType)
                .amount(new BigDecimal("0.001"))
                .build()
        );
        return Maybe.just(bets);
    }

    private Bet.Type getBetType(Round round) {
        BigDecimal ratio;
        ratio = round.downAmount().divide(round.upAmount(), RATIO_CONTEXT);
        if (ratio.compareTo(BigDecimal.ONE) > 0) {
            logger.info("Ratio is {} for {}, making bet for UP", ratio, round.number());
            return Bet.Type.UP;
        }
        ratio = round.upAmount().divide(round.downAmount(), RATIO_CONTEXT);
        if (ratio.compareTo(BigDecimal.ONE) > 0) {
            logger.info("Ratio is {} for {}, making bet for DOWN", ratio, round.number());
            return Bet.Type.DOWN;
        }
        return null;
    }
}
