package micdm.btce.test;

import io.reactivex.Flowable;
import io.reactivex.Single;
import micdm.btce.BetHandler;
import micdm.btce.BetMaker;
import micdm.btce.DataProvider;
import micdm.btce.models.Bet;
import micdm.btce.models.Round;
import micdm.btce.models.RoundBet;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.Collection;

class TestBetHandler implements BetHandler {

    static class Pair {
        final Round round;
        final RoundBet roundBet;
        Pair(Round round, RoundBet roundBet) {
            this.round = round;
            this.roundBet = roundBet;
        }
    }

    private final BigDecimal PRIZE_PART = new BigDecimal("0.95");

    private final BalanceBuffer balanceBuffer;
    private final BetMaker betMaker;
    private final DataProvider dataProvider;
    private final Logger logger;

    TestBetHandler(BalanceBuffer balanceBuffer, BetMaker betMaker, DataProvider dataProvider, Logger logger) {
        this.balanceBuffer = balanceBuffer;
        this.dataProvider = dataProvider;
        this.betMaker = betMaker;
        this.logger = logger;
    }

    void init() {
        Flowable<Pair> pairs = Flowable
            .zip(
                dataProvider.getRounds(),
                betMaker.getBets(),
                Pair::new
            )
            .share();
        pairs
            .doOnNext(pair -> {
                for (Bet bet: pair.roundBet.bets()) {
                    logger.info("Making bet for round {}: {} for {}", pair.roundBet.number(), bet.amount(), bet.type());
                }
            })
            .map(pair -> getRoundBalance(pair.round, pair.roundBet))
            .filter(delta -> !delta.equals(BigDecimal.ZERO))
            .subscribe(balanceBuffer::changeBalance);
        Single
            .zip(
                pairs.map(pair -> getWonBetCount(pair.round, pair.roundBet)).reduce(0, Integer::sum),
                pairs.map(pair -> getLostBetCount(pair.round, pair.roundBet)).reduce(0, Integer::sum),
                (wins, loses) -> (double) wins / loses
            )
            .subscribe(rate ->
                logger.info("Win rate is {}", rate)
            );
    }

    private BigDecimal getRoundBalance(Round round, RoundBet roundBet) {
        assert round.number() == roundBet.number();
        BigDecimal downAmount = round.downAmount();
        BigDecimal upAmount = round.upAmount();
        for (Bet bet: roundBet.downBets()) {
            downAmount = downAmount.add(bet.amount());
        }
        for (Bet bet: roundBet.upBets()) {
            upAmount = upAmount.add(bet.amount());
        }
        if (round.endPrice().compareTo(round.startPrice()) == -1) {
            return getPrize(roundBet.downBets(), downAmount, roundBet.upBets(), upAmount);
        } else {
            return getPrize(roundBet.upBets(), upAmount, roundBet.downBets(), downAmount);
        }
    }

    private BigDecimal getPrize(Collection<Bet> winningBets, BigDecimal winningAmount, Collection<Bet> losingBets, BigDecimal losingAmount) {
        BigDecimal prize = BigDecimal.ZERO;
        for (Bet bet: winningBets) {
            prize = prize.add(
                bet.amount()
                    .divide(winningAmount, 8, BigDecimal.ROUND_HALF_UP)
                    .multiply(losingAmount)
                    .multiply(PRIZE_PART)
            );
        }
        for (Bet bet: losingBets) {
            prize = prize.subtract(bet.amount());
        }
        return prize;
    }

    private int getWonBetCount(Round round, RoundBet roundBet) {
        if (round.endPrice().compareTo(round.startPrice()) < 0) {
            return roundBet.downBets().size();
        }
        return roundBet.upBets().size();
    }

    private int getLostBetCount(Round round, RoundBet roundBet) {
        if (round.endPrice().compareTo(round.startPrice()) < 0) {
            return roundBet.upBets().size();
        }
        return roundBet.downBets().size();
    }
}
