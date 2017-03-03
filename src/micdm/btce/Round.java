package micdm.btce;

import org.immutables.value.Value;

import java.math.BigDecimal;

@Value.Immutable
interface Round {

    int number();
    BigDecimal startPrice();
    BigDecimal endPrice();
    int downCount();
    BigDecimal downAmount();
    int upCount();
    BigDecimal upAmount();
}
