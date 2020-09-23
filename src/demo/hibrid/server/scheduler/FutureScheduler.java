package demo.hibrid.server.scheduler;

import demo.hibrid.request.Command;
import demo.hibrid.request.CommandResult;
import demo.hibrid.request.Request;
import demo.hibrid.request.Response;
import demo.hibrid.server.ExecutorInterface;
import demo.hibrid.server.HibridReplier;
import demo.hibrid.server.graph.ConflictDefinition;
import demo.hibrid.stats.Stats;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.*;

public class FutureScheduler {

    private final ExecutorInterface executor;
    private final HibridReplier replier;
    private final Partition[] partitions;

//    private final ForkJoinPool workerPool;
//    private final ForkJoinPool partitionPool;

    public FutureScheduler(HibridReplier replier,
                           ExecutorInterface executor,
                           int numPartitions,
                           int cosSize,
                           int workerThreads,
                           ConflictDefinition<Command> conflictDefinition) {
//        this.workerPool = new ForkJoinPool(workerThreads);
//        this.partitionPool = new ForkJoinPool(numPartitions);
        this.replier = replier;
        this.executor = executor;
        this.partitions = new Partition[numPartitions];
        for (int i = 0; i < partitions.length; i++) {
            partitions[i] = new Partition(conflictDefinition, cosSize);
        }
    }

    public CompletableFuture<Void> processRequest(Request request) {
        int requestId = request.getId();
        Command[] commands = request.getCommands();
        CompletableFuture<CommandResult>[] futureResults = new CompletableFuture[commands.length];
        for (int i = 0; i < commands.length; i++) {
            CompletableFuture<CommandResult> futureResult = futureResults[i] = new CompletableFuture<>();
            Command command = commands[i];
            supplyAsync(() -> schedule(command, futureResult))
                    .thenAcceptAsync((dependencies) -> dependencies.forEach(CompletableFuture::join))
                    .thenRunAsync(() -> execute(command, futureResult, requestId));
        }
        return runAsync(()-> manageReply(requestId, futureResults));
    }


    private List<CompletableFuture<?>> schedule(Command command, CompletableFuture<CommandResult> futureResult) {
        List<CompletableFuture<?>> dependencies = new LinkedList<>();
        command.distinctPartitions().forEach(it -> dependencies.addAll(partitions[it].add(command, futureResult)));
        return dependencies;
    }

    private void execute(Command command, CompletableFuture<CommandResult> commandExecution, int requestId) {
        boolean[] result = executor.execute(command);
        commandExecution.complete(new CommandResult(requestId, result));
        command.distinctPartitions().forEach(it -> partitions[it].releaseSpace());
    }

    private void manageReply(int requestId, CompletableFuture<CommandResult>[] futureResults) {
        CommandResult[] results = Arrays.stream(futureResults).map(CompletableFuture::join).toArray(CommandResult[]::new);
        var response = new Response(requestId, results);
        replier.reply(response);
    }


}

