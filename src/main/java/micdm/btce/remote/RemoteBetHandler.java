package micdm.btce.remote;

import com.google.gson.Gson;
import micdm.btce.BetHandler;
import micdm.btce.BetMaker;
import micdm.btce.models.Bet;
import micdm.btce.models.RoundBet;
import okhttp3.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;

class RemoteBetHandler implements BetHandler {

    private static class RoundBetAndCsrfToken {

        final RoundBet roundBet;
        final String csrfToken;

        RoundBetAndCsrfToken(RoundBet roundBet, String csrfToken) {
            this.roundBet = roundBet;
            this.csrfToken = csrfToken;
        }
    }

    private static class BetResult {

        final int error;
        final String errorText;
        final String data;

        BetResult(int error, String errorText, String data) {
            this.error = error;
            this.errorText = errorText;
            this.data = data;
        }
    }

    private static final String BETTING_URL = "https://btc-e.nz/ajax/betting";
    private static final String CSRF_TOKEN_FIELD = "csrfToken";
    private static final String ACTION_FIELD = "act";
    private static final String PERIOD_ID_FIELD = "period_id";
    private static final String CURRENCY_FIELD = "currency";
    private static final String BET_TYPE_FIELD = "type";
    private static final String BET_AMOUNT_FIELD = "amount";

    private final Authenticator authenticator;
    private final BetMaker betMaker;
    private final CsrfTokenProvider csrfTokenProvider;
    private final Gson gson;
    private final Logger logger;
    private final OkHttpClient httpClient;

    RemoteBetHandler(Authenticator authenticator, BetMaker betMaker, CsrfTokenProvider csrfTokenProvider, Gson gson, Logger logger, OkHttpClient httpClient) {
        this.betMaker = betMaker;
        this.authenticator = authenticator;
        this.csrfTokenProvider = csrfTokenProvider;
        this.gson = gson;
        this.logger = logger;
        this.httpClient = httpClient;
    }

    void init() {
        betMaker.getBets()
            .withLatestFrom(authenticator.run(), (roundBet, u) -> roundBet)
            .withLatestFrom(csrfTokenProvider.getToken(), RoundBetAndCsrfToken::new)
            .subscribe(roundBetAndCsrfToken -> {
                for (Bet bet: roundBetAndCsrfToken.roundBet.downBets()) {
                    makeBet(roundBetAndCsrfToken.roundBet.number(), bet.type(), bet.amount(), roundBetAndCsrfToken.csrfToken);
                }
                for (Bet bet: roundBetAndCsrfToken.roundBet.upBets()) {
                    makeBet(roundBetAndCsrfToken.roundBet.number(), bet.type(), bet.amount(), roundBetAndCsrfToken.csrfToken);
                }
            });
    }

    private void makeBet(int round, Bet.Type type, BigDecimal amount, String csrfToken) {
        logger.info("Making bet for round {}: {} for {}", round, amount, type);
        httpClient
            .newCall(
                new Request.Builder()
                    .post(
                        new FormBody.Builder()
                            .add(CSRF_TOKEN_FIELD, csrfToken)
                            .add(ACTION_FIELD, "bet")
                            .add(PERIOD_ID_FIELD, String.valueOf(round))
                            .add(CURRENCY_FIELD, "2")
                            .add(BET_TYPE_FIELD, type == Bet.Type.DOWN ? "1" : "0")
                            .add(BET_AMOUNT_FIELD, String.valueOf(amount))
                            .build()
                    )
                    .url(BETTING_URL)
                    .build()
            )
            .enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    logger.warn("Cannot make bet", e);
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    BetResult result = gson.fromJson(response.body().string(), BetResult.class);
                    if (result.error == 0) {
                        logger.info("Bet has been successfully made");
                    } else {
                        logger.warn("Cannot make bet: {}", result.errorText);
                    }
                }
            });
    }
}
