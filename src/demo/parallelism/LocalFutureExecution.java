package demo.parallelism;

import demo.hibrid.request.Command;
import demo.hibrid.request.Request;
import demo.hibrid.request.Response;
import demo.hibrid.server.HibridReplier;
import demo.hibrid.server.ListExecutor;
import demo.hibrid.server.graph.ConflictDefinition;
import demo.hibrid.server.graph.LockFreeNode;
import demo.hibrid.server.scheduler.FutureScheduler;
import demo.hibrid.stats.Stats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;


public class LocalFutureExecution {

    private final ListExecutor executor;
    private final HibridReplier replier;
    private final FutureScheduler scheduler;
    private final ConflictDefinition<Command> conflictDefinition;
    private long startTimestampInNanos;

    public static void main(String[] args) {
        if (args.length == 7) {
            Integer tamLista = Integer.valueOf(args[0]);
            Integer numWorkerThreads = Integer.valueOf(args[1]);
            Integer numParticoes = Integer.valueOf(args[2]);
            Integer percTransacoesGlobais = Integer.valueOf(args[3]);
            Integer percEscritas = Integer.valueOf(args[4]);
            Integer numeroOperacoes = Integer.valueOf(args[5]);
            Integer tamParticoes = Integer.valueOf(args[6]);
            System.out.println("Teste com futures!");
            System.out.print("{ n partitions: " + numParticoes);
            System.out.print(", n workers: " + numWorkerThreads);
            System.out.print(", list size: " + tamLista);
            System.out.print(", perc globals: " + percTransacoesGlobais);
            System.out.print(", perc writes: " + percEscritas);
            System.out.print(", n ops: " + numeroOperacoes);
            System.out.print(", cos size: " + tamParticoes);

            assert Stats.numOperations(numeroOperacoes);
            var localHibridExecution2 = new LocalFutureExecution(numParticoes, numWorkerThreads, tamLista, tamParticoes);
            Command[] commands = CommandCreator.createCommands(numeroOperacoes , percTransacoesGlobais, percEscritas, numParticoes, tamLista);

            CompletableFuture<?> response = localHibridExecution2.scheduleCommands(commands);
            response.join();

        } else {
            System.out.println("Modo de uso: java demo.parallelism.LocalFutureExecution <numClients> <tamListas> <numWorkerThreads> <numParticoes> <percTransacoesGlobais> <percEscritas> <numOperacoes> <tamParticoes>");
        }

    }


    public LocalFutureExecution(int numParticoes,
                                int numWorkerThreads,
                                int tamLista,
                                int tamParticoes) {
        this.executor = new ListExecutor(tamLista, numParticoes);
        this.conflictDefinition = (c1, c2) -> c1.type == Command.ADD || c2.type == Command.ADD;
        this.replier = new FakeHibridReplier();
        this.scheduler = new FutureScheduler(replier, executor, numParticoes, tamParticoes, numWorkerThreads, conflictDefinition);
    }


    public CompletableFuture<Void> scheduleCommands(Command[] commands) {
        startTimestampInNanos = System.nanoTime();
        return scheduler.processRequest(new Request(0,0,commands));
    }




    private String calculateThroughput(long start, long end, long numOperations) {
        var nanos = end - start;
        BigDecimal seconds = BigDecimal.valueOf(nanos).divide(BigDecimal.valueOf(1_000_000_000));
        BigDecimal tp = BigDecimal.valueOf(numOperations).divide(seconds, RoundingMode.HALF_DOWN);
        return String.valueOf(tp);
    }


    class FakeHibridReplier implements HibridReplier {

        @Override
        public void manageReply(LockFreeNode node, boolean[] results) {
            throw new UnsupportedOperationException("Não é para chamar esse método!");
        }

        @Override
        public void reply(Response response) {
            long endTimeInNanos = System.nanoTime();
            String throughput = calculateThroughput(startTimestampInNanos, endTimeInNanos, response.getResults().length);
            System.out.println("}: Throughput=" + throughput);
            assert Stats.print();
        }
    }

}

