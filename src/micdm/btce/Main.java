package micdm.btce;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Main {

    public static void main(String[] args) {
        for (int i = 0; i < 15; i += 1) {
            System.out.println(i);
            run(i);
        }
    }

    private static void run(int delta) {
        Gson gson = new GsonBuilder()
            .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Round.class, new RoundTypeAdapter())
            .create();
        RoundProvider roundProvider = new TestRoundProvider(gson);
        BetMaker betMaker = new BetMaker(roundProvider, new BetStrategy2(delta));
        TestBetHandler betHandler = new TestBetHandler(roundProvider, betMaker);
        betHandler.init();
    }
}
