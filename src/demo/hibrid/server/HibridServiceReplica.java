/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static demo.hibrid.stats.EventType.*;

/**
 * @author aldenio
 */
public class HibridServiceReplica extends AbstractServiceReplica implements HibridReplier {

    private final Map<Integer, HibridRequestContext> context = new ConcurrentHashMap<>();
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
    protected void processRequest(Request request) {
        var requestId = request.getId();
        var commands = request.getCommands();

        Stats.log(new Event(MESSAGE_RECEIVED, requestId, null, null, null));
        context.put(requestId, new HibridRequestContext(request));
        earlyScheduler.schedule(requestId, commands);
        Stats.log(new Event(MESSAGE_SCHEDULED, requestId, null, null, null));
    }

    public void manageReply(ServerCommand serverCommand, boolean[] results) {
        var ctx = context.get(serverCommand.requestId);
        var commandResult = new CommandResult(serverCommand.command.id, results);
        ctx.add(commandResult);
        if (ctx.isRequestFinished()) {
            var response = new Response(serverCommand.requestId, ctx.getResults());
            reply(response);
            context.remove(serverCommand.requestId);
            Stats.log(new Event(REPLY_SENT, serverCommand.requestId, null, null, null));
        }
    }


    /////////////////////////////////////////////////////////////////////
    // Métodos para criar e executar as threads dos schedulers e workes.
    ////////////////////////////////////////////////////////////////////
    public void start() {
        initLateSchedulers();
        initReplicaWorkers();
    }

    private void initLateSchedulers() {
        for (int i = 0; i < lateSchedulers.length; i++) {
            lateSchedulers[i] = new LateScheduler(i, queuesManager.getQueue(i), cosManager);
            lateSchedulers[i].start();
        }
    }

    private void initReplicaWorkers() {
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new HibridWorker(id, i, i % cosManager.getNumCOS(), cosManager, executor, hibridReplier);
            workers[i].start();
        }
    }

}
