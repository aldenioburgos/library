package demo.hibrid.server;

import demo.hibrid.client.HibridListClient;
import demo.hibrid.client.ServerProxy;
import demo.hibrid.request.Command;
import demo.hibrid.request.CommandResult;
import demo.hibrid.request.Request;
import demo.hibrid.server.graph.COSManager;
import demo.hibrid.server.graph.ConflictDefinitionDefault;
import demo.hibrid.server.scheduler.EarlyScheduler;
import demo.hibrid.server.scheduler.LateScheduler;
import demo.hibrid.server.scheduler.QueuesManager;
import demo.hibrid.stats.Stats;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class HibridServiceReplicaTest extends HibridExecutor implements HibridReplier, ServerProxy {
    static final int TEST_SIZE = 2000;
    static final int NUM_PARTITIONS = 2;

    QueuesManager queuesManager = new QueuesManager(NUM_PARTITIONS, 5);
    COSManager cosManager = new COSManager(NUM_PARTITIONS, 10, new ConflictDefinitionDefault());
    EarlyScheduler earlyScheduler = new EarlyScheduler(queuesManager);
    LateScheduler lateSchedulers[] = createLateSchedulers(4);
    HibridWorker worker0 = new HibridWorker(0, 0, 0, cosManager, this, this);
    HibridWorker worker1 = new HibridWorker(0, 1, 1, cosManager, this, this);

    Queue<ServerCommand> completedCommands = new ConcurrentLinkedQueue<>();

    public HibridServiceReplicaTest() throws InterruptedException, IOException {
        super();
        Stats.createInstance();
        start();
        start(worker0, worker1);
        start(lateSchedulers);
        createClient().start();
    }

    @Override
    public boolean[] execute(Command command) {
        return new boolean[]{false, false};
    }

    public LateScheduler[] createLateSchedulers(int numSchedulers) {
        var schedulers = new LateScheduler[numSchedulers];
        for (int i = 0; i < numSchedulers; i++) {
            schedulers[i] = new LateScheduler(cosManager, queuesManager, i, i % NUM_PARTITIONS);
        }
        return schedulers;
    }

    private void start(Thread... threads) {
        for (Thread thread : threads) {
            thread.start();
        }
    }

    public static void main(String[] args) throws Exception {
        var x = new HibridServiceReplicaTest();
    }

    public HibridListClient createClient() {
        var client = new HibridListClient(0, 2, TEST_SIZE, 0, 10000, 10, NUM_PARTITIONS, new int[]{50, 50}, new int[]{90, 10}, new int[]{20, 20});
        client.setServerProxy(this);
        return client;
    }

    @Override
    public CommandResult[] execute(int clientProcessId, int id, Command... commands) throws InterruptedException {
        var request = new Request(clientProcessId, id, commands);
        Stats.messageReceive(request);
        earlyScheduler.schedule(request.getId(), commands);
        return null;
    }

    AtomicBoolean print = new AtomicBoolean(false);
    @Override
    public void manageReply(ServerCommand serverCommand, boolean[] results) {
        completedCommands.add(serverCommand);
        if (completedCommands.size() == TEST_SIZE && print.compareAndSet(false, true)) {
            Stats.print();
        }
    }
}
