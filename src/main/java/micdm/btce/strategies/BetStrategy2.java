package micdm.btce.strategies;

import micdm.btce.models.Bet;
import micdm.btce.models.Round;
import micdm.btce.Config;
import micdm.btce.models.ImmutableBet;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

// Стратегия 2: вслед за большинством ставок
public class BetStrategy2 implements BetStrategy {

    private final Logger logger;

    public BetStrategy2(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Set<Bet> getBets(Round round) {
        Optional<Bet.Type> type = getType(round);
        if (!type.isPresent()) {
            return Collections.emptySet();
        }
        Set<Bet> bets = new HashSet<>();
        bets.add(
            ImmutableBet.builder()
                .type(type.get())
                .amount(Config.BET_AMOUNT)
                .build()
        );
        return bets;
    }

    private Optional<Bet.Type> getType(Round round) {
        int delta = Math.abs(round.downCount() - round.upCount());
        if (delta < Config.BET_COUNT_DELTA) {
            logger.info("Bet count delta is too low ({}), skipping", delta);
            return Optional.empty();
        }
        if (round.downCount() > round.upCount()) {
            logger.info("Making bet for DOWN ({} vs {})", round.downCount(), round.upCount());
            return Optional.of(Bet.Type.DOWN);
        } else {
            logger.info("Making bet for UP ({} vs {})", round.downCount(), round.upCount());
            return Optional.of(Bet.Type.UP);
        }
    }
}
