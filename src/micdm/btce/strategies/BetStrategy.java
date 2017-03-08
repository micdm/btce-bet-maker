package micdm.btce.strategies;

import micdm.btce.models.Bet;
import micdm.btce.models.Round;

import java.util.Set;

public interface BetStrategy {

    Set<Bet> getBets(Round round);
}
