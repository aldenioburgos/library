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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static demo.parallelism.util.ThreadUtil.join;
import static demo.parallelism.util.ThreadUtil.start;


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
//    volatile static boolean stop = false;

    public static void main(String[] args) {
        if (args.length == 7) {
            var numParticoes = Integer.valueOf(args[0]);
            var numWorkerThreads = Integer.valueOf(args[1]);
            var tamLista = Integer.valueOf(args[2]);
            var percTransacoesGlobais = Integer.valueOf(args[3]);
            var percEscritas = Integer.valueOf(args[4]);
            var numeroOperacoes = Integer.valueOf(args[5]);
            var tamParticoes = Integer.valueOf(args[6]);

            System.out.println("n partitions: " + numParticoes);
            System.out.println("n workers: " + numWorkerThreads);
            System.out.println("list size: " + tamLista);
            System.out.println("perc globals: " + percTransacoesGlobais);
            System.out.println("perc writes: " + percEscritas);
            System.out.println("n ops: " + numeroOperacoes);
            System.out.println("cos size: " + tamParticoes);

            var localHibridExecution2 = new LocalHibridExecution(numParticoes, numWorkerThreads, tamLista, percTransacoesGlobais, percEscritas, numeroOperacoes, tamParticoes);
            System.out.println("Criação dos comandos finalizada!");
            localHibridExecution2.startServerThreads();
            localHibridExecution2.scheduleCommands();
            localHibridExecution2.joinServerThreads();

//            while(!stop) {}
//            System.out.println(localHibridExecution2.cosManager);
//            System.out.println(localHibridExecution2.queuesManager);
//            System.exit(1);
        } else {
            System.out.println("Modo de uso: java demo.parallelism.LocalHibridExecution <numParticoes> <numWorkerThreads> <tamListas> <percTransacoesGlobais> <percEscritas> <numOperacoes> <tamParticoes>");
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
        this.queuesManager = new QueuesManager(numParticoes, numeroOperacoes);
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
            lateSchedulers[i] = new LateScheduler(i, queuesManager.queues[i], cosManager.graphs[i]);
        }
        return lateSchedulers;
    }


    public void scheduleCommands() {
        System.out.println("Enviando " + commandsLeft.get() + " comandos.");
        var request = new Request(0, 0, commands);
        startTimestampInNanos = System.nanoTime();
        earlyScheduler.schedule(request.getId(), commands);
        System.out.println("Fim do envio!");
    }

    @Override
    public void manageReply(CommandEnvelope commandEnvelope, boolean[] results) {
        int howManyCommandsAreLeft = commandsLeft.decrementAndGet();
        if (howManyCommandsAreLeft == 0) {
            long endTimeInNanos = System.nanoTime();
            String throughput = calculateThroughput(startTimestampInNanos, endTimeInNanos, commands.length);
            System.out.println("TP [partitions: " + cosManager.graphs.length + ", late workers: " + hibridWorkers.length + "] :" + throughput);
            System.exit(0);
        } else {
            if (howManyCommandsAreLeft % 1000 == 0) {
                System.out.print(howManyCommandsAreLeft + ", ");
            }
        }
    }

    private String calculateThroughput(long start, long end, long numOperations) {
        var nanos = end - start;
        var seconds = BigDecimal.valueOf(nanos).divide(BigDecimal.valueOf(1_000_000_000));
        var tp = BigDecimal.valueOf(numOperations).divide(seconds, RoundingMode.HALF_DOWN);
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

