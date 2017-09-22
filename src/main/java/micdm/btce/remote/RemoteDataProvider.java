package micdm.btce.remote;

import com.google.gson.Gson;
import com.neovisionaries.ws.client.*;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.Scheduler;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import micdm.btce.DataProvider;
import micdm.btce.models.ImmutableRound;
import micdm.btce.models.Round;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoteDataProvider implements DataProvider {

    private static class WebsocketDisconnectedException extends RuntimeException {}

    private enum MessageType {

        INIT(null, "pusher:connection_established"),
        SUBSCRIPTION(null, "pusher_internal:subscription_succeeded"),
        NEW_PERIOD("periods", "new_period"),
        PERIOD_UPDATE("periods", "update"),
        PERIOD_TIME("periods", "time"),
        TRADES("btc_usd.trades", "trades"),
        ACCOUNT_UPDATE(null, "update");

        final String channel;
        final String event;

        MessageType(String channel, String event) {
            this.channel = channel;
            this.event = event;
        }

        boolean is(String channel, String event) {
            if (this.channel != null) {
                return this.channel.equals(channel) && this.event.equals(event);
            }
            return this.event.equals(event);
        }
    }

    private static class Message {

        final String channel;
        final String event;
        final String data;

        Message(String channel, String event, String data) {
            this.channel = channel;
            this.event = event;
            this.data = data;
        }
    }

    private static class NewPeriodData {

        final int id;
        final int pairId;
        final int currency;
        final BigDecimal basePrice;
        final String timeStr;
        final int timeLeft;

        NewPeriodData(int id, int pairId, int currency, BigDecimal basePrice, String timeStr, int timeLeft) {
            this.id = id;
            this.pairId = pairId;
            this.currency = currency;
            this.basePrice = basePrice;
            this.timeStr = timeStr;
            this.timeLeft = timeLeft;
        }
    }

    private static class PeriodUpdateData {

        final int periodId;
        final BigDecimal betsUpTotal;
        final BigDecimal betsDownTotal;
        final String betsCountStr;

        PeriodUpdateData(int periodId, BigDecimal betsUpTotal, BigDecimal betsDownTotal, String betsCountStr) {
            this.periodId = periodId;
            this.betsUpTotal = betsUpTotal;
            this.betsDownTotal = betsDownTotal;
            this.betsCountStr = betsCountStr;
        }
    }

    private static class PeriodTimeData {

        final int minutes;

        PeriodTimeData(int minutes) {
            this.minutes = minutes;
        }
    }

    private static class TradeData {

        final BigDecimal price;

        TradeData(BigDecimal price) {
            this.price = price;
        }
    }

    private static class AccountUpdateData {

        final Map<String, BigDecimal> balance;

        private AccountUpdateData(Map<String, BigDecimal> balance) {
            this.balance = balance;
        }
    }

    private static class Datas {

        final NewPeriodData newPeriodData;
        final PeriodUpdateData periodUpdateData;
        final long endsIn;

        Datas(NewPeriodData newPeriodData, PeriodUpdateData periodUpdateData, long endsIn) {
            this.newPeriodData = newPeriodData;
            this.periodUpdateData = periodUpdateData;
            this.endsIn = endsIn;
        }
    }

    private static final Pattern BET_COUNT_PATTERN = Pattern.compile("&#8593;(\\d+) &#8595;(\\d+)");
    private static final int PAIR_ID = 1;
    private static final int CURRENCY_ID = 2;

    private final AccountIdProvider accountIdProvider;
    private final Gson gson;
    private final Logger logger;
    private final Scheduler ioScheduler;
    private final WebSocketFactory websocketFactory;

    private Flowable<Object> messages;
    private final FlowableProcessor<Round> rounds = PublishProcessor.create();

    RemoteDataProvider(AccountIdProvider accountIdProvider, Gson gson, Scheduler ioScheduler, Logger logger, WebSocketFactory websocketFactory) {
        this.accountIdProvider = accountIdProvider;
        this.gson = gson;
        this.logger = logger;
        this.ioScheduler = ioScheduler;
        this.websocketFactory = websocketFactory;
    }

    @Override
    public void init() {
        Flowable
            .combineLatest(
                getPeriodDatas(),
                getMessages().ofType(TradeData.class),
                (datas, tradeData) -> buildRound(datas.newPeriodData, datas.periodUpdateData, datas.endsIn, tradeData)
            )
            .subscribe(rounds);
    }

    @Override
    public Flowable<Round> getRounds() {
        return rounds;
    }

    private Flowable<Object> getMessages() {
        if (messages == null) {
            messages = accountIdProvider.getAccountId()
                .flatMap(accountId ->
                    Flowable
                        .create(source -> receiveMessages(source, accountId), BackpressureStrategy.BUFFER)
                        .retry(error -> error instanceof WebsocketDisconnectedException)
                        .subscribeOn(ioScheduler)
                )
                .share();
        }
        return messages;
    }

    private void receiveMessages(FlowableEmitter<Object> source, String accountId) throws Exception {
        logger.info("Connecting to websocket...");
        WebSocket websocket = websocketFactory.createSocket("wss://ws.pusherapp.com/app/c354d4d129ee0faa5c92?protocol=6&client=js&version=2.0.0&flash=false");
        websocket.addListener(new WebSocketAdapter() {
            @Override
            public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
                logger.info("Websocket connected");
            }
            @Override
            public void onTextMessage(WebSocket websocket, String text) throws Exception {
                Message message = gson.fromJson(text, Message.class);
                if (MessageType.INIT.is(message.channel, message.event)) {
                    handleInitMessage(websocket, accountId);
                }
                if (MessageType.SUBSCRIPTION.is(message.channel, message.event)) {
                    handleSubscriptionMessage(message);
                }
                if (MessageType.NEW_PERIOD.is(message.channel, message.event)) {
                    handleNewPeriodMessage(message, source);
                }
                if (MessageType.PERIOD_UPDATE.is(message.channel, message.event)) {
                    handlePeriodUpdateMessage(message, source);
                }
                if (MessageType.PERIOD_TIME.is(message.channel, message.event)) {
                    handlePeriodTimeMessage(message, source);
                }
                if (MessageType.TRADES.is(message.channel, message.event)) {
                    handleTradesMessage(message, source);
                }
                if (MessageType.ACCOUNT_UPDATE.is(message.channel, message.event) && message.channel.equals(accountId)) {
                    handleAccountUpdateMessage(message, source);
                }
            }
            @Override
            public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                source.onError(cause);
            }
            @Override
            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
                if (closedByServer) {
                    logger.warn("Websocket disconnected");
                    source.onError(new WebsocketDisconnectedException());
                } else {
                    source.onComplete();
                }
            }
        });
        websocket.connect();
        source.setCancellable(websocket::disconnect);
    }

    private void handleInitMessage(WebSocket websocket, String accountId) {
        logger.trace("Init event received");
        websocket.sendText("{\"event\":\"pusher:subscribe\",\"data\":{\"channel\":\"periods\"}}");
        websocket.sendText("{\"event\":\"pusher:subscribe\",\"data\":{\"channel\":\"btc_usd.trades\"}}");
        websocket.sendText(String.format("{\"event\":\"pusher:subscribe\",\"data\":{\"channel\":\"%s\"}}", accountId));
    }

    private void handleSubscriptionMessage(Message message) {
        logger.trace("Successfully subscribed to {}", message.channel);
    }

    private void handleNewPeriodMessage(Message message, FlowableEmitter<Object> source) {
        NewPeriodData data = gson.fromJson(message.data, NewPeriodData.class);
        logger.trace("New period: {}", data);
        source.onNext(data);
    }

    private void handlePeriodUpdateMessage(Message message, FlowableEmitter<Object> source) {
        PeriodUpdateData data = gson.fromJson(message.data, PeriodUpdateData.class);
        logger.trace("Period update: {}", data);
        source.onNext(data);
    }

    private void handlePeriodTimeMessage(Message message, FlowableEmitter<Object> source) {
        PeriodTimeData data = new PeriodTimeData(Integer.valueOf(message.data));
        logger.trace("Period time: {}", data);
        source.onNext(data);
    }

    private void handleTradesMessage(Message message, FlowableEmitter<Object> source) {
        Object[][] temp = gson.fromJson(message.data, Object[][].class);
        TradeData data = new TradeData(new BigDecimal((String) temp[temp.length - 1][1]));
        logger.trace("Trade: {}", data);
        source.onNext(data);
    }

    private void handleAccountUpdateMessage(Message message, FlowableEmitter<Object> source) {
        AccountUpdateData data = gson.fromJson(message.data, AccountUpdateData.class);
        logger.trace("Account update: {}", data);
        source.onNext(data);
    }

    private Round buildRound(NewPeriodData newPeriodData, PeriodUpdateData periodUpdateData, Long endsIn, TradeData tradeData) {
        ImmutableRound.Builder builder = ImmutableRound.builder()
            .number(newPeriodData.id)
            .startPrice(newPeriodData.basePrice)
            .endPrice(tradeData.price)
            .startTime(getRoundStartTime(newPeriodData.timeStr))
            .endsIn(Duration.standardSeconds(endsIn));
        if (periodUpdateData != null) {
            Matcher matcher = BET_COUNT_PATTERN.matcher(periodUpdateData.betsCountStr);
            if (!matcher.find()) {
                throw new IllegalStateException("cannot parse bet counts");
            }
            builder
                .downCount(Integer.valueOf(matcher.group(2)))
                .downAmount(periodUpdateData.betsDownTotal)
                .upCount(Integer.valueOf(matcher.group(1)))
                .upAmount(periodUpdateData.betsUpTotal);
        } else {
            builder
                .downCount(0)
                .downAmount(BigDecimal.ZERO)
                .upCount(0)
                .upAmount(BigDecimal.ZERO);
        }
        return builder.build();
    }

    private DateTime getRoundStartTime(String raw) {
        String[] parts = raw.split("-");
        return LocalTime.parse(parts[0]).toDateTimeToday(DateTimeZone.forID("Europe/Moscow")).withZone(DateTimeZone.UTC);
    }

    private Flowable<Datas> getPeriodDatas() {
        return getMessages()
            .ofType(NewPeriodData.class)
            .filter(data -> data.pairId == PAIR_ID && data.currency == CURRENCY_ID)
            .switchMap(newPeriodData ->
                Flowable.combineLatest(
                    getMessages()
                        .ofType(PeriodUpdateData.class)
                        .filter(data -> data.periodId == newPeriodData.id)
                        .filter(data -> data.betsCountStr != null)
                        .map(Optional::of)
                        .startWith(Optional.empty()),
                    getEndsIn(newPeriodData.timeLeft),
                    (periodUpdateData, endsIn) -> new Datas(newPeriodData, periodUpdateData.isPresent() ? periodUpdateData.get() : null, endsIn)
                )
            );
    }

    private Flowable<Long> getEndsIn(int initial) {
        return Flowable
            .merge(
                Flowable.just(initial * 60),
                getMessages()
                    .ofType(PeriodTimeData.class)
                    .map(data -> data.minutes * 60)
            )
            .switchMap(seconds ->
                Flowable.interval(0, 1, TimeUnit.SECONDS).map(counter -> seconds - counter)
            );
    }

    @Override
    public Flowable<BigDecimal> getBalance() {
        return getMessages().ofType(AccountUpdateData.class).map(accountUpdateData -> accountUpdateData.balance.get("2"));
    }
}
