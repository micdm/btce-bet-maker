package micdm.btce.models;

import org.immutables.value.Value;

import java.math.BigDecimal;

@Value.Immutable
public interface Bet {

    enum Type {
        DOWN,
        UP,
    }

    Type type();
    BigDecimal amount();
}
