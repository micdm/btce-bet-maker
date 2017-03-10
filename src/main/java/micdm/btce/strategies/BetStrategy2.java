package micdm.btce.strategies;

import micdm.btce.models.Bet;
import micdm.btce.models.Round;
import micdm.btce.Config;
import micdm.btce.models.ImmutableBet;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

// Стратегия 2: вслед за большинством ставок
class BetStrategy2 implements BetStrategy {

    private final Config config;
    private final Logger logger;

    BetStrategy2(Config config, Logger logger) {
        this.config = config;
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
                .amount(getAmount(round, type.get()))
                .build()
        );
        return bets;
    }

    private Optional<Bet.Type> getType(Round round) {
        int delta = Math.abs(round.downCount() - round.upCount());
        if (delta < config.BET_COUNT_DELTA) {
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

    private BigDecimal getAmount(Round round, Bet.Type betType) {
        return config.BET_AMOUNT;
    }
}
