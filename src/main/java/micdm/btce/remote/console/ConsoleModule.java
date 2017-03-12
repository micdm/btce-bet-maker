package micdm.btce.remote.console;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import org.slf4j.Logger;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.ServerSocket;

@Module
public class ConsoleModule {

    private final int bindPort;

    public ConsoleModule(int bindPort) {
        this.bindPort = bindPort;
    }

    @Provides
    @Singleton
    UserConsole provideUserConsole(Logger logger, @Named("newThread") Scheduler newThreadScheduler, ServerSocket serverSocket) {
        UserConsole instance = new UserConsole(logger, newThreadScheduler, serverSocket);
        instance.init();
        return instance;
    }

    @Provides
    @Singleton
    ServerSocket provideServerSocket() {
        try {
            return new ServerSocket(bindPort);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("cannot bind to %s", bindPort));
        }
    }
}
