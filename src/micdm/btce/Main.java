package micdm.btce;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    public static void main(String[] args) {
        Gson gson = new GsonBuilder()
            .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
        Logger logger = LoggerFactory.getLogger("main");
        DataProvider dataProvider = new RemoteDataProvider(gson, logger, Schedulers.io());
        dataProvider.getRounds()
            .filter(round -> round.endsIn().getStandardSeconds() % 20 == 0 || round.endsIn().getStandardSeconds() < 10)
            .subscribe(round -> logger.info("Round: {}", round));
        dataProvider.getBalance()
            .subscribe(balance -> logger.info("Balance: {}", balance));
        MainThreadExecutor executor = new MainThreadExecutor(logger);
        executor.run();
    }
}
