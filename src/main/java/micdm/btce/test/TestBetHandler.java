package micdm.btce.test;

import io.reactivex.Flowable;
import micdm.btce.BetHandler;
import micdm.btce.BetMaker;
import micdm.btce.DataProvider;
import micdm.btce.models.Bet;
import micdm.btce.models.Round;
import micdm.btce.models.RoundBet;

import java.math.BigDecimal;
import java.util.Set;

class TestBetHandler implements BetHandler {

    private final BigDecimal PRIZE_PART = new BigDecimal("0.95");

    private final BalanceBuffer balanceBuffer;
    private final BetMaker betMaker;
    private final DataProvider dataProvider;

    TestBetHandler(BalanceBuffer balanceBuffer, BetMaker betMaker, DataProvider dataProvider) {
        this.balanceBuffer = balanceBuffer;
        this.dataProvider = dataProvider;
        this.betMaker = betMaker;
    }

    void init() {
        Flowable
            .zip(
                dataProvider.getRounds(),
                betMaker.getBets(),
                this::getRoundBalance
            )
            .filter(delta -> !delta.equals(BigDecimal.ZERO))
            .subscribe(balanceBuffer::changeBalance);
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

    private BigDecimal getPrize(Set<Bet> winningBets, BigDecimal winningAmount, Set<Bet> losingBets, BigDecimal losingAmount) {
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
}
