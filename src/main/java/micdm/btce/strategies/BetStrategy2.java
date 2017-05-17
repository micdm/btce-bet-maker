package micdm.btce.strategies;

import io.reactivex.Maybe;
import micdm.btce.Config;
import micdm.btce.models.Bet;
import micdm.btce.models.ImmutableBet;
import micdm.btce.models.Round;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

// Стратегия 2: вслед за большинством ставок
class BetStrategy2 implements BetStrategy {

    private static final MathContext RATIO_MAX_CONTEXT = new MathContext(6, RoundingMode.DOWN);

    private final Config config;
    private final Config.Strategy2Config strategyConfig;
    private final Logger logger;

    BetStrategy2(Config config, Config.Strategy2Config strategyConfig, Logger logger) {
        this.config = config;
        this.strategyConfig = strategyConfig;
        this.logger = logger;
    }

    @Override
    public Maybe<Collection<Bet>> getBets(Round round) {
        Optional<Bet.Type> type = getType(round);
        if (!type.isPresent()) {
            return Maybe.empty();
        }
        Optional<BigDecimal> amount = getAmount(round, type.get());
        if (!amount.isPresent()) {
            return Maybe.empty();
        }
        Collection<Bet> bets = new HashSet<>();
        bets.add(
            ImmutableBet.builder()
                .type(type.get())
                .amount(amount.get())
                .build()
        );
        return Maybe.just(bets);
    }

    private Optional<Bet.Type> getType(Round round) {
        int delta = Math.abs(round.downCount() - round.upCount());
        if (delta < strategyConfig.BET_COUNT_DELTA) {
            logger.info("Bet count delta is too low ({}), skipping", delta);
            return Optional.empty();
        }
        if (round.downCount() > round.upCount()) {
            logger.info("Making bet for DOWN ({} vs {}) for round {}", round.downCount(), round.upCount(), round.number());
            return Optional.of(Bet.Type.DOWN);
        } else {
            logger.info("Making bet for UP ({} vs {}) for round {}", round.downCount(), round.upCount(), round.number());
            return Optional.of(Bet.Type.UP);
        }
    }

    private Optional<BigDecimal> getAmount(Round round, Bet.Type betType) {
        BigDecimal ratio;
        if (betType == Bet.Type.DOWN) {
            ratio = round.upAmount().divide(round.downAmount(), RATIO_MAX_CONTEXT);
        } else {
            ratio = round.downAmount().divide(round.upAmount(), RATIO_MAX_CONTEXT);
        }
        logger.debug("Ratio is {} ({} / {})", ratio, round.downAmount(), round.upAmount());
        if (ratio.compareTo(strategyConfig.MIN_RATIO) < 0) {
            logger.debug("Ratio is too small, skipping");
            return Optional.empty();
        }
        return Optional.of((ratio.compareTo(BigDecimal.ONE)) > 0 ? strategyConfig.MAX_BET_AMOUNT : strategyConfig.MIN_BET_AMOUNT);
    }
}
