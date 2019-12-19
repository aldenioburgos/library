package demo.hibrid.server;

import demo.hibrid.client.HibridClientConfig;
import demo.hibrid.client.HibridListClient;
import demo.hibrid.client.ServerProxy;
import demo.hibrid.client.ServerProxyFactory;
import demo.hibrid.request.Command;
import demo.hibrid.request.CommandResult;
import demo.hibrid.request.Request;
import demo.hibrid.server.graph.COSManager;
import demo.hibrid.server.graph.ConflictDefinitionDefault;
import demo.hibrid.server.scheduler.EarlyScheduler;
import demo.hibrid.server.scheduler.LateScheduler;
import demo.hibrid.server.queue.QueuesManager;
import demo.hibrid.stats.Stats;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class HibridServiceReplicaTest  implements HibridReplier, ServerProxy {

    //Configurações comuns.
    private static final int NUM_PARTITIONS = 2;

    // Configurações só do cliente
    private static final int TEST_SIZE = 2000;
    private static final int NUM_OPERATIONS_PER_REQUEST = 50;
    private static final int[] PERCENTUAL_OPERATIONS_PER_PARTITION = new int[]{50, 50};
    private static final int[] PERCENTUAL_PARTITIONS_ENVOLVED_IN_EACH_OPERATION = new int[]{50, 50};
    private static final int[] WRITES_PER_PARTITION = new int[]{20, 20};

    // Configurações só do servidor

    private static final int MIN_READ_TIME = 1;
    private static final int MAX_READ_TIME = 5;
    private static final int MIN_WRITE_TIME = 5;
    private static final int MAX_WRITE_TIME = 10;
    private static final int MAX_QUEUE_SIZE = 5;
    private static final int MAX_COS_SIZE = 10;
    private static final int NUM_WORKERS = 2;





    private QueuesManager queuesManager = new QueuesManager(NUM_PARTITIONS, MAX_QUEUE_SIZE);
    private COSManager cosManager = new COSManager(NUM_PARTITIONS, MAX_COS_SIZE, new ConflictDefinitionDefault());
    private EarlyScheduler earlyScheduler = new EarlyScheduler(queuesManager);
    private HibridExecutor executor = new HibridExecutor(MIN_READ_TIME, MAX_READ_TIME, MIN_WRITE_TIME, MAX_WRITE_TIME);


    private Queue<ServerCommand> completedCommands = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) throws Exception {
        Stats.createInstance();
        new HibridServiceReplicaTest();
    }

    public HibridServiceReplicaTest() throws InterruptedException, IOException {
        HibridWorker[] workers = createWorkers();
        LateScheduler[] lateSchedulers = createLateSchedulers();
        start(workers);
        start(lateSchedulers);
        createClient().start();
    }

    private HibridWorker[] createWorkers() {
        var workers = new HibridWorker[NUM_WORKERS];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new HibridWorker(0, i, i, cosManager, executor, this);
        }
        return workers;
    }

    private LateScheduler[] createLateSchedulers() {
        var schedulers = new LateScheduler[NUM_PARTITIONS];
        for (int i = 0; i < schedulers.length; i++) {
            schedulers[i] = new LateScheduler(cosManager, queuesManager, i, i % NUM_PARTITIONS, i % NUM_PARTITIONS);
        }
        return schedulers;
    }

    private HibridListClient createClient() {
        var config = new HibridClientConfig(TEST_SIZE, NUM_OPERATIONS_PER_REQUEST, NUM_PARTITIONS, PERCENTUAL_OPERATIONS_PER_PARTITION, PERCENTUAL_PARTITIONS_ENVOLVED_IN_EACH_OPERATION, WRITES_PER_PARTITION);
        return new HibridListClient(config, new ServerProxyFactory(this));
    }

    private void start(Thread... threads) {
        for (Thread thread : threads) {
            thread.start();
        }
    }

    /**
     * Esse método faz o papel do ServiceReplica.
     */
    @Override
    public CommandResult[] execute(int processId, int workerId, Command... commands)  {
        var request = new Request(processId, workerId, commands);
        Stats.messageReceive(request);
        earlyScheduler.schedule(request.getId(), commands);
        Stats.messageScheduled(request);
        return null;
    }

    /**
     * Esse método faz o papel do ServiceReplica
     */
    @Override
    public void manageReply(ServerCommand serverCommand, boolean[] results) {
        completedCommands.add(serverCommand);
        if (completedCommands.size() == TEST_SIZE && print.compareAndSet(false, true)) {
            Stats.print();
            System.exit(0);
        }
    }
    private AtomicBoolean print = new AtomicBoolean(false);
}
