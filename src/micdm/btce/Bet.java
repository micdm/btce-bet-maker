package micdm.btce;

import org.immutables.value.Value;

import java.math.BigDecimal;

@Value.Immutable
interface Bet {

    enum Type {
        DOWN,
        UP,
    }

    Type type();
    BigDecimal amount();
}
