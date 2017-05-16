package micdm.btce.strategies;

import io.reactivex.Maybe;
import micdm.btce.Config;
import micdm.btce.models.Bet;
import micdm.btce.models.ImmutableBet;
import micdm.btce.models.Round;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashSet;

// Стратегия 6: решает нейронная сеть
class BetStrategy6 implements BetStrategy {

    private final Config config;
    private final Logger logger;
    private final NeuralNetworkConnector neuralNetworkConnector;

    BetStrategy6(Config config, Logger logger, NeuralNetworkConnector neuralNetworkConnector) {
        this.config = config;
        this.logger = logger;
        this.neuralNetworkConnector = neuralNetworkConnector;
    }

    @Override
    public Maybe<Collection<Bet>> getBets(Round round) {
        return neuralNetworkConnector.getProbabilities(round.downCount(), round.downAmount(), round.upCount(), round.upAmount(),
                                                       round.startTime().getDayOfWeek(), round.startTime().getMinuteOfDay())
            .map(probabilities -> {
                Collection<Bet> bets = new HashSet<>();
                bets.add(
                    ImmutableBet.builder()
                        .type(probabilities.decrease < probabilities.increase ? Bet.Type.DOWN : Bet.Type.UP)
                        .amount(config.MIN_BET_AMOUNT)
                        .build()
                );
                return bets;
            })
            .toMaybe()
            .onErrorResumeNext(Maybe.empty());
    }
}
