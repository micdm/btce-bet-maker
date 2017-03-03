package micdm.btce;

import io.reactivex.Flowable;

class BetMaker {

    private final RoundProvider roundProvider;
    private final BetStrategy betStrategy;

    BetMaker(RoundProvider roundProvider, BetStrategy betStrategy) {
        this.roundProvider = roundProvider;
        this.betStrategy = betStrategy;
    }

    Flowable<RoundBet> getBets() {
        return roundProvider.getRounds().map(round -> {
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
