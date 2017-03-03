package micdm.btce;

import java.util.Set;

interface BetStrategy {

    Set<Bet> getBets(Round round);
}
