package micdm.btce.remote.console;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.stream.Stream;

public class UserConsole {

    private final Logger logger;
    private final Scheduler newThreadScheduler;
    private final ServerSocket serverSocket;

    private final FlowableProcessor<UserCommand> commands = PublishProcessor.create();

    UserConsole(Logger logger, Scheduler newThreadScheduler, ServerSocket serverSocket) {
        this.logger = logger;
        this.newThreadScheduler = newThreadScheduler;
        this.serverSocket = serverSocket;
    }

    void init() {
        getClients()
            .compose(getCommandsFromSocket())
            .subscribe(commands::onNext);
    }

    private Flowable<Socket> getClients() {
        return Flowable
            .<Socket>create(source -> {
                while (!serverSocket.isClosed()) {
                    source.onNext(serverSocket.accept());
                }
                source.onComplete();
            }, BackpressureStrategy.BUFFER)
            .subscribeOn(newThreadScheduler);
    }

    private FlowableTransformer<Socket, UserCommand> getCommandsFromSocket() {
        return flowable -> flowable.flatMap(socket ->
            Flowable
                .<UserCommand>create(source -> {
                    logger.info("New console client: {}", socket);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    while (!socket.isClosed()) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        String[] parts = line.split(" ");
                        if (parts.length != 2) {
                            logger.warn("Unknown command: {}", line);
                            writer.write("BAD COMMAND\n");
                            writer.flush();
                            continue;
                        }
                        Optional<UserCommand> command = Stream.of(UserCommand.values())
                            .filter(item -> item.is(parts[0], parts[1]))
                            .findFirst();
                        if (command.isPresent()) {
                            writer.write("OK\n");
                            writer.flush();
                            source.onNext(command.get());
                        } else {
                            writer.write("BAD COMMAND\n");
                            writer.flush();
                        }
                    }
                    logger.info("Console client {} has left", socket);
                    source.onComplete();
                }, BackpressureStrategy.BUFFER)
                .doOnNext(command -> logger.info("New console command: {}", command))
                .subscribeOn(newThreadScheduler)
        );
    }

    public Flowable<UserCommand> getCommands() {
        return commands;
    }
}
