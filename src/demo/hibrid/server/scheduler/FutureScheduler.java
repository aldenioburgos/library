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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.allOf;

public class FutureScheduler {

    private final ExecutorInterface executor;
    private final HibridReplier replier;
    private final Partition[] partitions;
    private final Semaphore space;
    private final ForkJoinPool workerPool;
    private final ForkJoinPool partitionPool;

    public FutureScheduler(HibridReplier replier,
                           ExecutorInterface executor,
                           int numPartitions,
                           int cosSize,
                           int workerThreads,
                           ConflictDefinition<Command> conflictDefinition) {
        this.space = new Semaphore(cosSize);
        this.workerPool = new ForkJoinPool(workerThreads);
        this.partitionPool = new ForkJoinPool(numPartitions);
        this.replier = replier;
        this.executor = executor;
        this.partitions = new Partition[numPartitions];
        for (int i = 0; i < partitions.length; i++) {
            partitions[i] = new Partition(conflictDefinition);
        }
    }

    public void processRequest(Request request) {
        int requestId = request.getId();
        Command[] commands = request.getCommands();
        List<CompletableFuture<CommandResult>> results = new ArrayList<>(commands.length);
        for (Command command : commands) {
            assert Stats.cosSize(space.availablePermits());
            acquireSpace();
            results.add(schedule(requestId, command));
        }
        allOf(results.toArray(CompletableFuture[]::new))
                .thenRun(() -> manageReply(requestId, results));
    }

    private CompletableFuture<CommandResult> schedule(int requestId, Command command) {
        CompletableFuture<CommandResult> commandRun = new CompletableFuture<>();
        List<CompletableFuture<?>> dependencies1 = new LinkedList<>();
        List<CompletableFuture<?>> dependencies2 = new LinkedList<>();
        int[] distinctPartitions = command.distinctPartitions();
        for (int i = 0; i < distinctPartitions.length; i++) {
            Partition partition = partitions[distinctPartitions[i]];
            CompletableFuture<Void> inserirNaParticao = CompletableFuture.runAsync(()-> dependencies2.addAll(partition.add(command, commandRun)), partitionPool);
            dependencies1.add(inserirNaParticao);
        }

        return allOf(dependencies1.toArray(CompletableFuture[]::new))
                .thenCombineAsync(allOf(dependencies2.toArray(CompletableFuture[]::new)), (Void a, Void b) -> null)
                .thenCombineAsync(CompletableFuture.supplyAsync(() -> execute(commandRun, command, requestId), workerPool), (a, b) -> b);
    }

    private CommandResult execute(CompletableFuture<CommandResult> commandRun, Command command, int requestId) {
        var result = new CommandResult(requestId, executor.execute(command));
        commandRun.complete(result);
        releaseSpace();
        return result;
    }

    private void manageReply(int requestId, List<CompletableFuture<CommandResult>> futureResults) {
        List<CommandResult> results = futureResults.stream().map(CompletableFuture::join).collect(Collectors.toList());
        var response = new Response(requestId, results.toArray(CommandResult[]::new));
        replier.reply(response);
    }

    private void releaseSpace() {
        this.space.release();
    }

    private void acquireSpace() {
        try {
            this.space.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}

