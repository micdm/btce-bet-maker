package micdm.btce;

import io.reactivex.Flowable;
import micdm.btce.misc.CommonFunctions;
import micdm.btce.models.ImmutableRoundBet;
import micdm.btce.models.Round;
import micdm.btce.models.RoundBet;
import micdm.btce.strategies.BetStrategy;
import org.joda.time.Duration;

public class BetMaker {

    private static final Duration TIME_BEFORE_END = Duration.standardSeconds(3);

    private final BetStrategy betStrategy;
    private final CommonFunctions commonFunctions;
    private final DataProvider dataProvider;
    private final SystemSettings systemSettings;

    BetMaker(BetStrategy betStrategy, CommonFunctions commonFunctions, DataProvider dataProvider, SystemSettings systemSettings) {
        this.commonFunctions = commonFunctions;
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
            .concatMap(round ->
                betStrategy.getBets(round)
                    .map(bets ->
                        ImmutableRoundBet.builder()
                            .number(round.number())
                            .bets(bets)
                            .build()
                    )
                    .toFlowable()
            );
    }
}
