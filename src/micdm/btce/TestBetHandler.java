package micdm.btce;

import io.reactivex.Single;

import java.math.BigDecimal;
import java.util.Set;

class TestBetHandler {

    private final BigDecimal PRIZE_PART = new BigDecimal("0.95");

    private final RoundProvider roundProvider;
    private final BetMaker betMaker;

    TestBetHandler(RoundProvider roundProvider, BetMaker betMaker) {
        this.roundProvider = roundProvider;
        this.betMaker = betMaker;
    }

    void init() {
        Single
            .zip(
                getBalance(),
                getBetCount(),
                (balance, count) -> String.format("Balance is %s, bet count is %s, effectiveness is %s", balance, count, balance.divide(new BigDecimal(count), 6, BigDecimal.ROUND_HALF_UP))
            )
            .subscribe(System.out::println);
        getRoundCount().subscribe(count ->
            System.out.printf("Total round count is %s\n", count)
        );
    }

    private Single<BigDecimal> getBalance() {
        return roundProvider.getRounds()
            .flatMap(round ->
                betMaker.getBets().filter(roundBet -> roundBet.number() == round.number()),
                this::getRoundBalance
            )
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getRoundBalance(Round round, RoundBet roundBet) {
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

    private Single<Integer> getBetCount() {
        return betMaker.getBets().reduce(0, (result, roundBet) -> result + roundBet.downBets().size() + roundBet.upBets().size());
    }

    private Single<Long> getRoundCount() {
        return roundProvider.getRounds().count();
    }
}
