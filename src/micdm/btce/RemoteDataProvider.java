package micdm.btce;

import com.google.gson.Gson;
import com.neovisionaries.ws.client.*;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.Scheduler;
import org.joda.time.Duration;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RemoteDataProvider implements DataProvider {

    private enum MessageType {

        INIT(null, "pusher:connection_established") {
            @Override
            void execute(WebSocket websocket, Message message, FlowableEmitter<Object> source, RemoteDataProvider handler) {
                handler.handleInitMessage(websocket, message, source);
            }
        },
        SUBSCRIPTION(null, "pusher_internal:subscription_succeeded") {
            @Override
            void execute(WebSocket websocket, Message message, FlowableEmitter<Object> source, RemoteDataProvider handler) {
                handler.handleSubscriptionMessage(websocket, message, source);
            }
        },
        NEW_PERIOD("periods", "new_period") {
            @Override
            void execute(WebSocket websocket, Message message, FlowableEmitter<Object> source, RemoteDataProvider handler) {
                handler.handleNewPeriodMessage(websocket, message, source);
            }
        },
        PERIOD_UPDATE("periods", "update") {
            @Override
            void execute(WebSocket websocket, Message message, FlowableEmitter<Object> source, RemoteDataProvider handler) {
                handler.handlePeriodUpdateMessage(websocket, message, source);
            }
        },
        PERIOD_TIME("periods", "time") {
            @Override
            void execute(WebSocket websocket, Message message, FlowableEmitter<Object> source, RemoteDataProvider handler) {
                handler.handlePeriodTimeMessage(websocket, message, source);
            }
        },
        TRADES("btc_usd.trades", "trades") {
            @Override
            void execute(WebSocket websocket, Message message, FlowableEmitter<Object> source, RemoteDataProvider handler) {
                handler.handleTradeMessage(websocket, message, source);
            }
        },
        ACCOUNT_UPDATE(Config.ACCOUNT_ID, "update") {
            @Override
            void execute(WebSocket websocket, Message message, FlowableEmitter<Object> source, RemoteDataProvider handler) {
                handler.handleAccountUpdateMessage(websocket, message, source);
            }
        };

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

        abstract void execute(WebSocket websocket, Message message, FlowableEmitter<Object> source, RemoteDataProvider handler);
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
        final BigDecimal basePrice;
        final int timeLeft;

        NewPeriodData(int id, BigDecimal basePrice, int timeLeft) {
            this.id = id;
            this.basePrice = basePrice;
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

        final BigDecimal balance2;

        AccountUpdateData(BigDecimal balance2) {
            this.balance2 = balance2;
        }
    }

    private static final Pattern BET_COUNT_PATTERN = Pattern.compile("&#8593;(\\d+) &#8595;(\\d+)");

    private final Gson gson;
    private final Logger logger;
    private final Scheduler ioScheduler;

    private Flowable<Object> messages;

    RemoteDataProvider(Gson gson, Logger logger, Scheduler ioScheduler) {
        this.gson = gson;
        this.logger = logger;
        this.ioScheduler = ioScheduler;
    }

    @Override
    public Flowable<Round> getRounds() {
        return Flowable
            .combineLatest(
                getMessages()
                    .ofType(NewPeriodData.class)
                    .switchMap(newPeriodData -> {
                        ImmutableRound.Builder builder = ImmutableRound.builder()
                            .number(newPeriodData.id)
                            .startPrice(newPeriodData.basePrice)
                            .downCount(0)
                            .downAmount(BigDecimal.ZERO)
                            .upCount(0)
                            .upAmount(BigDecimal.ZERO);
                        return getMessages()
                            .ofType(PeriodUpdateData.class)
                            .filter(periodUpdateData -> periodUpdateData.periodId == newPeriodData.id)
                            .map(periodUpdateData -> {
                                if (periodUpdateData.betsCountStr == null) {
                                    return builder;
                                }
                                Matcher matcher = BET_COUNT_PATTERN.matcher(periodUpdateData.betsCountStr);
                                if (!matcher.find()) {
                                    throw new IllegalStateException("cannot parse bet counts");
                                }
                                return builder
                                    .downCount(Integer.valueOf(matcher.group(2)))
                                    .downAmount(periodUpdateData.betsDownTotal)
                                    .upCount(Integer.valueOf(matcher.group(1)))
                                    .upAmount(periodUpdateData.betsUpTotal);
                            })
                            .startWith(builder);
                    }),
                getMessages().ofType(TradeData.class),
                Flowable
                    .merge(
                        getMessages()
                            .ofType(NewPeriodData.class)
                            .map(newPeriodData -> newPeriodData.timeLeft * 60),
                        getMessages()
                            .ofType(PeriodTimeData.class)
                            .map(periodTimeData -> periodTimeData.minutes * 60)
                    )
                    .switchMap(seconds ->
                        Flowable.interval(1, TimeUnit.SECONDS)
                            .map(counter -> seconds - counter)
                    ),
                (builder, tradeData, endsIn) ->
                    builder
                        .endPrice(tradeData.price)
                        .endsIn(Duration.standardSeconds(endsIn))
                        .build()
            );
    }

    private Flowable<Object> getMessages() {
        if (messages == null) {
            messages = Flowable.create(this::receiveMessages, BackpressureStrategy.BUFFER)
                .subscribeOn(ioScheduler)
                .share();
        }
        return messages;
    }

    private void receiveMessages(FlowableEmitter<Object> source) throws Exception {
        logger.debug("Connecting to websocket...");
        WebSocketFactory factory = new WebSocketFactory();
        WebSocket websocket = factory.createSocket("wss://ws.pusherapp.com/app/c354d4d129ee0faa5c92?protocol=6&client=js&version=2.0.0&flash=false");
        websocket.addListener(new WebSocketAdapter() {
            @Override
            public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
                logger.debug("Websocket connected");
            }
            @Override
            public void onTextMessage(WebSocket websocket, String text) throws Exception {
                Message message = gson.fromJson(text, Message.class);
                for (MessageType messageType: MessageType.values()) {
                    if (messageType.is(message.channel, message.event)) {
                        messageType.execute(websocket, message, source, RemoteDataProvider.this);
                        break;
                    }
                }
            }
            @Override
            public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                source.onError(cause);
            }
            @Override
            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
                logger.debug("Websocket disconnected");
                source.onComplete();
            }
        });
        websocket.connect();
        source.setCancellable(websocket::disconnect);
    }

    private void handleInitMessage(WebSocket websocket, Message message, FlowableEmitter<Object> source) {
        logger.debug("Init event received");
        websocket.sendText("{\"event\":\"pusher:subscribe\",\"data\":{\"channel\":\"periods\"}}");
        websocket.sendText("{\"event\":\"pusher:subscribe\",\"data\":{\"channel\":\"btc_usd.trades\"}}");
        websocket.sendText(String.format("{\"event\":\"pusher:subscribe\",\"data\":{\"channel\":\"%s\"}}", Config.ACCOUNT_ID));
    }

    private void handleSubscriptionMessage(WebSocket websocket, Message message, FlowableEmitter<Object> source) {
        logger.debug("Successfully subscribed to {}", message.channel);
    }

    private void handleNewPeriodMessage(WebSocket websocket, Message message, FlowableEmitter<Object> source) {
        NewPeriodData data = gson.fromJson(message.data, NewPeriodData.class);
        logger.debug("New period: {}", data);
        source.onNext(data);
    }

    private void handlePeriodUpdateMessage(WebSocket websocket, Message message, FlowableEmitter<Object> source) {
        PeriodUpdateData data = gson.fromJson(message.data, PeriodUpdateData.class);
        logger.debug("Period update: {}", data);
        source.onNext(data);
    }

    private void handlePeriodTimeMessage(WebSocket websocket, Message message, FlowableEmitter<Object> source) {
        PeriodTimeData data = new PeriodTimeData(Integer.valueOf(message.data));
        logger.debug("Period time: {}", data);
        source.onNext(data);
    }

    private void handleTradeMessage(WebSocket websocket, Message message, FlowableEmitter<Object> source) {
        Object[][] temp = gson.fromJson(message.data, Object[][].class);
        TradeData data = new TradeData(new BigDecimal((String) temp[temp.length - 1][1]));
        logger.debug("Trade: {}", data);
        source.onNext(data);
    }

    private void handleAccountUpdateMessage(WebSocket websocket, Message message, FlowableEmitter<Object> source) {
        AccountUpdateData data = gson.fromJson(message.data, AccountUpdateData.class);
        logger.debug("Account update: {}", data);
        source.onNext(data);
    }

    @Override
    public Flowable<BigDecimal> getBalance() {
        return getMessages().ofType(AccountUpdateData.class).map(accountUpdateData -> accountUpdateData.balance2);
    }
}
