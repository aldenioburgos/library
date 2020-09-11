package demo.parallelism;

import demo.hibrid.request.Command;
import demo.hibrid.request.Request;
import demo.hibrid.server.CommandEnvelope;
import demo.hibrid.server.HibridReplier;
import demo.hibrid.server.HibridWorker;
import demo.hibrid.server.ListExecutor;
import demo.hibrid.server.graph.COSManager;
import demo.hibrid.server.graph.ConflictDefinition;
import demo.hibrid.server.graph.ConflictDefinitionDefault;
import demo.hibrid.server.queue.QueuesManager;
import demo.hibrid.server.scheduler.EarlyScheduler;
import demo.hibrid.server.scheduler.LateScheduler;
import demo.hibrid.stats.Stats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static demo.util.Utils.join;
import static demo.util.Utils.start;


public class LocalHibridExecution implements HibridReplier {

    private final EarlyScheduler earlyScheduler;
    private final LateScheduler[] lateSchedulers;
    private final QueuesManager queuesManager;
    private final COSManager cosManager;
    private final ListExecutor executor;
    private final HibridWorker[] hibridWorkers;
    private final HibridReplier hibridReplier;
    private final ConflictDefinition<CommandEnvelope> conflictDefinition;
    private final Command[] commands;
    private final AtomicInteger commandsLeft;
    private long startTimestampInNanos;

    public static void main(String[] args) {
        if (args.length == 7) {
            var tamLista = Integer.valueOf(args[0]);
            var numWorkerThreads = Integer.valueOf(args[1]);
            var numParticoes = Integer.valueOf(args[2]);
            var percTransacoesGlobais = Integer.valueOf(args[3]);
            var percEscritas = Integer.valueOf(args[4]);
            var numeroOperacoes = Integer.valueOf(args[5]);
            var tamParticoes = Integer.valueOf(args[6]);
            System.out.println("sem o executor!");
            System.out.print("{ n partitions: " + numParticoes);
            System.out.print(", n workers: " + numWorkerThreads);
            System.out.print(", list size: " + tamLista);
            System.out.print(", perc globals: " + percTransacoesGlobais);
            System.out.print(", perc writes: " + percEscritas);
            System.out.print(", n ops: " + numeroOperacoes);
            System.out.print(", cos size: " + tamParticoes);

            var localHibridExecution2 = new LocalHibridExecution(numParticoes, numWorkerThreads, tamLista, percTransacoesGlobais, percEscritas, numeroOperacoes, tamParticoes);
            localHibridExecution2.startServerThreads();
            localHibridExecution2.scheduleCommands();
            localHibridExecution2.joinServerThreads();
        } else {
            System.out.println("Modo de uso: java demo.parallelism.LocalHibridExecution <tamListas> <numWorkerThreads> <numParticoes> <percTransacoesGlobais> <percEscritas> <numOperacoes> <tamParticoes>");
        }

    }

    private void joinServerThreads() {
        join(lateSchedulers);
        join(hibridWorkers);
    }

    private void startServerThreads() {
        start(lateSchedulers);
        start(hibridWorkers);
    }


    public LocalHibridExecution(Integer numParticoes,
                                Integer numWorkerThreads,
                                Integer tamLista,
                                Integer percTransacoesGlobais,
                                Integer percEscritas,
                                Integer numeroOperacoes,
                                Integer tamParticoes) {
        this.hibridReplier = this;
        this.executor = new ListExecutor(tamLista, numParticoes);
        this.queuesManager = new QueuesManager(numParticoes);
        this.conflictDefinition = new ConflictDefinitionDefault();
        this.cosManager = new COSManager(numParticoes, tamParticoes, conflictDefinition);
        this.earlyScheduler = new EarlyScheduler(queuesManager,cosManager);
        this.lateSchedulers = createLateSchedulers(numParticoes, queuesManager, cosManager);
        this.hibridWorkers = createWorkers(numWorkerThreads, numParticoes, cosManager, executor, hibridReplier);
        // criação dos comandos
        this.commands = CommandCreator.createCommands(numeroOperacoes, percTransacoesGlobais, percEscritas, numParticoes, tamLista);
        this.commandsLeft = new AtomicInteger(commands.length);
    }

    private HibridWorker[] createWorkers(int numWorkerThreads, int numPartitions, COSManager cosManager, ListExecutor executor, HibridReplier hibridReplier) {
        var workers = new HibridWorker[numWorkerThreads];
        for (int i = 0; i < workers.length; i++) {
            int preferentialPartition = i % numPartitions;
            workers[i] = new HibridWorker(i, preferentialPartition, cosManager, executor, hibridReplier);
        }
        return workers;
    }

    private LateScheduler[] createLateSchedulers(int numParticoes, QueuesManager queuesManager, COSManager cosManager) {
        var lateSchedulers = new LateScheduler[numParticoes];
        for (int i = 0; i < lateSchedulers.length; i++) {
            lateSchedulers[i] = new LateScheduler(i, queuesManager, cosManager);
        }
        return lateSchedulers;
    }


    public void scheduleCommands() {
        var request = new Request(0, 0, commands);
        startTimestampInNanos = System.nanoTime();
        earlyScheduler.schedule(request.getId(), commands);
    }

    @Override
    public void manageReply(CommandEnvelope commandEnvelope, boolean[] results) {
        int howManyCommandsAreLeft = commandsLeft.decrementAndGet();
        if (howManyCommandsAreLeft == 0) {
            long endTimeInNanos = System.nanoTime();
            String throughput = calculateThroughput(startTimestampInNanos, endTimeInNanos, commands.length);
            System.out.println("}: Throughput=" + throughput);
            assert Stats.print(commands.length);
            System.exit(0);
        }
    }

    private String calculateThroughput(long start, long end, long numOperations) {
        var nanos = end - start;
        BigDecimal seconds = BigDecimal.valueOf(nanos).divide(BigDecimal.valueOf(1_000_000_000));
        BigDecimal tp = BigDecimal.valueOf(numOperations).divide(seconds, RoundingMode.HALF_DOWN);
        return String.valueOf(tp);
    }


    @Override
    public String toString() {
        return "LocalHibridExecution2{" +
                "earlyScheduler=" + earlyScheduler +
                ", lateSchedulers=" + Arrays.toString(lateSchedulers) +
                ", queuesManager=" + queuesManager +
                ", cosManager=" + cosManager +
                ", executor=" + executor +
                ", hibridWorkers=" + Arrays.toString(hibridWorkers) +
                ", hibridReplier=" + hibridReplier +
                ", conflictDefinition=" + conflictDefinition +
                ", commands=" + Arrays.toString(commands) +
                ", commandsLeft=" + commandsLeft +
                '}';
    }
}

