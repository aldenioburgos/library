package demo.hibrid.server;

import demo.hibrid.request.CommandResult;
import demo.hibrid.request.Request;
import demo.hibrid.request.Response;
import demo.hibrid.server.graph.COSManager;
import demo.hibrid.server.graph.ConflictDefinitionDefault;
import demo.hibrid.server.queue.QueuesManager;
import demo.hibrid.server.scheduler.EarlyScheduler;
import demo.hibrid.server.scheduler.LateScheduler;
import demo.hibrid.stats.Event;
import demo.hibrid.stats.Stats;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static demo.hibrid.stats.EventType.*;

/**
 * @author aldenio
 */
public class HibridServiceReplica extends AbstractServiceReplica implements HibridReplier {

    protected final Map<Integer, HibridRequestContext> context = new ConcurrentHashMap<>();
    private final EarlyScheduler earlyScheduler;
    private final QueuesManager queuesManager;
    private final LateScheduler[] lateSchedulers;
    private final COSManager cosManager;
    private final HibridWorker[] workers;
    private final HibridExecutor executor;
    private final HibridReplier hibridReplier;

    public HibridServiceReplica(int id, int queueSize, int cosSize, int numPartitions, int numWorkers, HibridExecutor executor) {
        super(id, executor, null);
        this.queuesManager = new QueuesManager(numPartitions, queueSize);
        this.earlyScheduler = new EarlyScheduler(queuesManager);
        this.cosManager = new COSManager(numPartitions, cosSize, new ConflictDefinitionDefault());
        this.lateSchedulers = new LateScheduler[numPartitions];
        this.workers = new HibridWorker[numWorkers];
        this.executor = executor;
        this.hibridReplier =  this;
    }

    /////////////////////////////////////////////////////////////////////
    // Métodos que fazem o trabalho dessa replica
    ////////////////////////////////////////////////////////////////////
    public void processRequest(Request request) {
        var requestId = request.getId();
        var commands = request.getCommands();

//        Stats.log(new Event(MESSAGE_RECEIVED, requestId, null, null, null));
        context.put(requestId, new HibridRequestContext(request));
        earlyScheduler.schedule(requestId, commands);
//        Stats.log(new Event(MESSAGE_SCHEDULED, requestId, null, null, null));
    }

    public void manageReply(CommandEnvelope commandEnvelope, boolean[] results) {
        var ctx = context.get(commandEnvelope.requestId);
        var commandResult = new CommandResult(commandEnvelope.command.id, results);
        ctx.add(commandResult);
        if (ctx.isRequestFinished()) {
            var response = new Response(commandEnvelope.requestId, ctx.getResults());
            context.remove(commandEnvelope.requestId);
            reply(response);
            System.out.print('.');
//            Stats.log(new Event(REPLY_SENT, commandEnvelope.requestId, null, null, null));
        }
    }


    /////////////////////////////////////////////////////////////////////
    // Métodos para criar e executar as threads dos schedulers e workes.
    ////////////////////////////////////////////////////////////////////
    public Thread[] start() {
        var um = initLateSchedulers();
        var dois = initReplicaWorkers();
        var soma = new Thread[um.length+dois.length];
        System.arraycopy(um,0,soma, 0, um.length);
        System.arraycopy(dois, 0, soma, um.length,dois.length);
        return soma;
    }

    private Thread[] initLateSchedulers() {
        for (int i = 0; i < lateSchedulers.length; i++) {
            lateSchedulers[i] = new LateScheduler(i, queuesManager.queues[i], cosManager.graphs[i]);
            lateSchedulers[i].start();
        }
        return lateSchedulers;
    }

    private Thread[] initReplicaWorkers() {
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new HibridWorker(id, i, i % cosManager.graphs.length, cosManager, executor, hibridReplier);
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
