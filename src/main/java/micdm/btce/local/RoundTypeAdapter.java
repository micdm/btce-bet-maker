package micdm.btce.local;

import com.google.gson.*;
import micdm.btce.models.ImmutableRound;
import micdm.btce.models.Round;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.lang.reflect.Type;

class RoundTypeAdapter implements JsonDeserializer<Round> {

    @Override
    public Round deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        return ImmutableRound.builder()
            .number(object.get("number").getAsInt())
            .startPrice(object.get("start_price").getAsBigDecimal())
            .endPrice(object.get("end_price").getAsBigDecimal())
            .downCount(object.get("bets").getAsJsonArray().get(1).getAsInt())
            .downAmount(object.get("amounts").getAsJsonArray().get(1).getAsBigDecimal())
            .upCount(object.get("bets").getAsJsonArray().get(0).getAsInt())
            .upAmount(object.get("amounts").getAsJsonArray().get(0).getAsBigDecimal())
            .startTime(DateTime.parse(object.get("start_time").getAsString()))
            .endsIn(Duration.standardSeconds(1))
            .build();
    }
}
