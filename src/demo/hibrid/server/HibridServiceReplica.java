/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.server;

import bftsmart.tom.core.messages.TOMMessage;
import demo.hibrid.request.CommandResult;
import demo.hibrid.request.Request;
import demo.hibrid.request.Response;
import demo.hibrid.server.graph.COSManager;
import demo.hibrid.server.graph.ConflictDefinitionDefault;
import demo.hibrid.server.scheduler.EarlyScheduler;
import demo.hibrid.server.scheduler.LateScheduler;
import demo.hibrid.server.scheduler.QueuesManager;
import demo.hibrid.stats.Stats;
import parallelism.ParallelServiceReplica;

import java.util.HashMap;
import java.util.Map;

/**
 * @author aldenio
 */
public class HibridServiceReplica extends ParallelServiceReplica implements HibridReplier {

    private final EarlyScheduler earlyScheduler;
    private final QueuesManager queuesManager;
    private final COSManager cosManager;
    private final Map<Integer, HibridRequestContext> context = new HashMap<>();
    private final HibridExecutor executor;

    public HibridServiceReplica(int id, HibridExecutor executor, int numPartitions, int maxQueueSize, int numWorkers, int maxCOSSize) {
        super(id, executor, null);
        if (executor == null) throw new IllegalArgumentException("Invalid null argument Executor.");
        if (numPartitions <= 0) throw new IllegalArgumentException("Invalid zero or negative argument numPartitions.");
        if (maxQueueSize <= 0) throw new IllegalArgumentException("Invalid zero or negative argument maxQueueSize.");
        if (numWorkers <= 0) throw new IllegalArgumentException("Invalid zero or negative argument numWorkers.");
        if (maxCOSSize <= 0) throw new IllegalArgumentException("Invalid zero or negative argument maxCOSSize.");
        this.executor = executor;
        this.queuesManager = new QueuesManager(numPartitions, maxQueueSize);
        this.earlyScheduler = new EarlyScheduler(queuesManager);
        this.cosManager = new COSManager(numPartitions, maxCOSSize, new ConflictDefinitionDefault());
        initLateSchedulers(numPartitions);
        initReplicaWorkers(numWorkers, numPartitions);
    }

    private void initLateSchedulers(int numPartitions) {
        for (int i = 0; i < numPartitions; i++) {
            new LateScheduler(cosManager, queuesManager, i).start();
        }
    }

    private void initReplicaWorkers(int numWorkers, int numPartitions) {
        for (int i = 0; i < numWorkers; i++) {
            new HibridWorker(id, i, i % numPartitions, cosManager,  executor, this).start();
        }
    }

    public void manageReply(ServerCommand serverCommand, boolean[] results) {
        var requestId = serverCommand.requestId;
        var commandId = serverCommand.getCommandId();
        var ctx = context.get(requestId);
        var commandResult = new CommandResult(commandId, results);
        ctx.add(commandResult);
        System.out.println("REPLY => " + commandResult + " for " + serverCommand);
        System.out.println("CTX => " + ctx);
        if (ctx.finished()) {
            var response = new Response(requestId, ctx.getResults());
            ctx.message.reply = new TOMMessage(id, ctx.message.getSession(), ctx.message.getSequence(), response.toBytes(), SVController.getCurrentViewId());
            replier.manageReply(ctx.message, null);
            Stats.messageReply(requestId);
        }
    }

    protected void processOrderedRequest(TOMMessage message) {
        var request = new Request().fromBytes(message.getContent());
        var requestId = request.getId();
        var commands = request.getCommands();
        context.put(requestId, new HibridRequestContext(commands.length, message));
        processRequest(request);
    }

    public void processRequest(Request request) {
        try {
            Stats.messageReceive(request);
            earlyScheduler.schedule(request.getId(), request.getCommands());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
