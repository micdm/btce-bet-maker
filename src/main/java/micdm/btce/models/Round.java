package micdm.btce.models;

import org.immutables.value.Value;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.math.BigDecimal;

@Value.Immutable
public interface Round {

    int number();
    BigDecimal startPrice();
    BigDecimal endPrice();
    int downCount();
    BigDecimal downAmount();
    int upCount();
    BigDecimal upAmount();
    DateTime startTime();
    Duration endsIn();
}
