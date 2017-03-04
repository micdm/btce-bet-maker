package micdm.btce;

import org.slf4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

public class MainThreadExecutor implements Executor {

    private final Logger logger;
    private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();

    public MainThreadExecutor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void execute(Runnable task) {
        logger.debug("New task {} arrived", task);
        tasks.add(task);
    }

    public void run() {
        logger.info("Running executor");
        while (!Thread.interrupted()) {
            try {
                tasks.take().run();
            } catch (InterruptedException e) {
                logger.info("Executor interrupted");
                break;
            }
        }
    }
}
