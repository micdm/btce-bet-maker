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
import java.util.Collection;
import java.util.HashSet;

// Стратегия 6: решает нейронная сеть
class BetStrategy6 implements BetStrategy {

    private static final MathContext RATIO_CONTEXT = new MathContext(6, RoundingMode.DOWN);

    private final Config config;
    private final Config.Strategy6Config strategyConfig;
    private final Logger logger;
    private final NeuralNetworkConnector neuralNetworkConnector;

    BetStrategy6(Config config, Config.Strategy6Config strategyConfig, Logger logger, NeuralNetworkConnector neuralNetworkConnector) {
        this.config = config;
        this.strategyConfig = strategyConfig;
        this.logger = logger;
        this.neuralNetworkConnector = neuralNetworkConnector;
    }

    @Override
    public Maybe<Collection<Bet>> getBets(Round round) {
        return neuralNetworkConnector.getProbabilities(round.downCount(), round.downAmount(), round.upCount(), round.upAmount(),
                                                       round.startTime().getDayOfWeek(), round.startTime().getMinuteOfDay())
            .toMaybe()
            .onErrorResumeNext(Maybe.empty())
            .flatMap(probabilities -> {
                Bet.Type betType = getBetType(probabilities);
                BigDecimal ratio = getBetAmountRatio(round, betType);
                if (ratio.compareTo(strategyConfig.MIN_RATIO) <= 0) {
                    logger.debug("Ratio {} is too small, skipping", ratio);
                    return Maybe.empty();
                }
                Collection<Bet> bets = new HashSet<>();
                bets.add(
                    ImmutableBet.builder()
                        .type(betType)
                        .amount(strategyConfig.BET_AMOUNT)
                        .build()
                );
                return Maybe.just(bets);
            });
    }

    private Bet.Type getBetType(NeuralNetworkConnector.Probabilities probabilities) {
        return probabilities.decrease > probabilities.increase ? Bet.Type.DOWN : Bet.Type.UP;
    }

    private BigDecimal getBetAmountRatio(Round round, Bet.Type betType) {
        return betType == Bet.Type.DOWN ? round.upAmount().divide(round.downAmount(), RATIO_CONTEXT) : round.downAmount().divide(round.upAmount(), RATIO_CONTEXT);
    }
}
