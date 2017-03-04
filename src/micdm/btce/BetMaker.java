package micdm.btce;

import io.reactivex.Flowable;

class BetMaker {

    private final DataProvider dataProvider;
    private final BetStrategy betStrategy;

    BetMaker(DataProvider dataProvider, BetStrategy betStrategy) {
        this.dataProvider = dataProvider;
        this.betStrategy = betStrategy;
    }

    Flowable<RoundBet> getBets() {
        return dataProvider.getRounds().map(round -> {
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
