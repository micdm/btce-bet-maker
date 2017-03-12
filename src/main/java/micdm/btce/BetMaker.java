package micdm.btce;

import io.reactivex.Flowable;
import micdm.btce.models.Bet;
import micdm.btce.models.ImmutableRoundBet;
import micdm.btce.models.Round;
import micdm.btce.models.RoundBet;
import micdm.btce.strategies.BetStrategy;
import org.joda.time.Duration;

public class BetMaker {

    private static final Duration TIME_BEFORE_END = Duration.standardSeconds(3);

    private final BetStrategy betStrategy;
    private final DataProvider dataProvider;
    private final SystemSettings systemSettings;

    BetMaker(BetStrategy betStrategy, DataProvider dataProvider, SystemSettings systemSettings) {
        this.dataProvider = dataProvider;
        this.betStrategy = betStrategy;
        this.systemSettings = systemSettings;
    }

    public Flowable<RoundBet> getBets() {
        return systemSettings.isBettingEnabled()
            .switchMap(isBettingEnabled -> isBettingEnabled ? getBetsForRounds() : Flowable.never());
    }

    private Flowable<RoundBet> getBetsForRounds() {
        return dataProvider.getRounds()
            .filter(round -> round.endsIn().isShorterThan(TIME_BEFORE_END))
            .distinctUntilChanged(Round::number)
            .map(round -> {
                ImmutableRoundBet.Builder builder = ImmutableRoundBet.builder().number(round.number());
                for (Bet bet: betStrategy.getBets(round)) {
                    if (bet.type() == Bet.Type.DOWN) {
                        builder.addDownBets(bet);
                    }
                    if (bet.type() == Bet.Type.UP) {
                        builder.addUpBets(bet);
                    }
                }
                return builder.build();
            });
    }
}
