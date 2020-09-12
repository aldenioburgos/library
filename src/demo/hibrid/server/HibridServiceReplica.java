package demo.hibrid.server;

import demo.hibrid.request.Command;
import demo.hibrid.request.CommandResult;
import demo.hibrid.request.Request;
import demo.hibrid.request.Response;
import demo.hibrid.server.graph.COSManager;
import demo.hibrid.server.graph.ConflictDefinitionDefault;
import demo.hibrid.server.graph.LockFreeNode;
import demo.hibrid.server.queue.QueuesManager;
import demo.hibrid.server.scheduler.EarlyScheduler;
import demo.hibrid.server.scheduler.LateScheduler;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author aldenio
 */
public class HibridServiceReplica extends AbstractServiceReplica implements HibridReplier {

    public final Map<Integer, HibridRequestContext> context = new ConcurrentHashMap<>();
    public final EarlyScheduler earlyScheduler;
    private final QueuesManager queuesManager;
    private final LateScheduler[] lateSchedulers;
    private final COSManager cosManager;
    private final HibridWorker[] workers;
    private final ExecutorInterface executor;
    private final HibridReplier hibridReplier;

    public HibridServiceReplica(int id, int queueSize, int cosSize, int numPartitions, int numWorkers, ExecutorInterface executor) {
        super(id, executor, null);
        this.queuesManager = new QueuesManager(numPartitions);
        this.cosManager = new COSManager( cosSize, new ConflictDefinitionDefault());
        this.earlyScheduler = new EarlyScheduler(queuesManager, cosManager, numPartitions);
        this.lateSchedulers = new LateScheduler[numPartitions];
        this.workers = new HibridWorker[numWorkers];
        this.executor = executor;
        this.hibridReplier =  this;
    }

    /////////////////////////////////////////////////////////////////////
    // Métodos que fazem o trabalho dessa replica
    ////////////////////////////////////////////////////////////////////
    public void processRequest(Request request) {
        int requestId = request.getId();
        Command[] commands = request.getCommands();

        context.put(requestId, new HibridRequestContext(request));
        earlyScheduler.schedule(requestId, commands);
    }

    public void manageReply(LockFreeNode node, boolean[] results) {
        HibridRequestContext ctx = context.get(node.requestId);
        var commandResult = new CommandResult(node.command.id, results);
        ctx.add(commandResult);
        if (ctx.isRequestFinished()) {
            var response = new Response(node.requestId, ctx.getResults());
            context.remove(node.requestId);
            reply(response);
        }
    }


    /////////////////////////////////////////////////////////////////////
    // Métodos para criar e executar as threads dos schedulers e workes.
    ////////////////////////////////////////////////////////////////////
    public Thread[] start() {
        Thread[] um = initLateSchedulers();
        Thread[] dois = initReplicaWorkers();
        var soma = new Thread[um.length+dois.length];
        System.arraycopy(um,0,soma, 0, um.length);
        System.arraycopy(dois, 0, soma, um.length,dois.length);
        return soma;
    }

    private Thread[] initLateSchedulers() {
        for (int i = 0; i < lateSchedulers.length; i++) {
            lateSchedulers[i] = new LateScheduler(i, lateSchedulers.length, queuesManager, cosManager);
            lateSchedulers[i].start();
        }
        return lateSchedulers;
    }

    private Thread[] initReplicaWorkers() {
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new HibridWorker(i, i % lateSchedulers.length, cosManager, executor, hibridReplier);
            workers[i].start();
        }
        return workers;
    }

    @Override
    public String toString() {
        return "HibridServiceReplica{" +
                "\ncontext=" + context +
                ", \nearlyScheduler=" + earlyScheduler +
                ", \nlateSchedulers=" + Arrays.toString(lateSchedulers) +
                ", \ncosManager=" + cosManager +
                ", \nworkers=" + Arrays.toString(workers) +
                ", \nexecutor=" + executor +
                '}';
    }
}
