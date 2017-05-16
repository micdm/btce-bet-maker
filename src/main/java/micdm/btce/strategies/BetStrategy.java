package micdm.btce.strategies;

import io.reactivex.Maybe;
import micdm.btce.models.Bet;
import micdm.btce.models.Round;

import java.util.Collection;

public interface BetStrategy {

    Maybe<Collection<Bet>> getBets(Round round);
}
