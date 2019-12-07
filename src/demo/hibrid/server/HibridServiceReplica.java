/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.server;

import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.util.ThroughputStatistics;
import demo.hibrid.request.Command;
import demo.hibrid.request.CommandResult;
import demo.hibrid.request.Request;
import demo.hibrid.request.Response;
import demo.hibrid.server.graph.ConflictDefinitionDefault;
import demo.hibrid.server.scheduler.COSManager;
import demo.hibrid.server.scheduler.EarlyScheduler;
import demo.hibrid.server.scheduler.LateScheduler;
import demo.hibrid.server.scheduler.QueuesManager;
import parallelism.ParallelServiceReplica;

import java.util.HashMap;
import java.util.Map;

/**
 * @author eduardo
 */
public class HibridServiceReplica extends ParallelServiceReplica {

    private final EarlyScheduler earlyScheduler;
    private final QueuesManager queuesManager;
    private final LateScheduler[] lateSchedulers;
    private final COSManager cosManager;
    private final Map<Integer, HibridRequestContext> context = new HashMap<>();
    private final HibridExecutor executor;

    public HibridServiceReplica(int id, HibridExecutor executor, int numPartitions, int maxQueueSize, int numWorkers, int maxCOSSize) {
        super(id, executor, null);
        this.executor = executor;
        this.queuesManager = new QueuesManager(numPartitions, maxQueueSize);
        this.earlyScheduler = new EarlyScheduler(queuesManager);
        this.lateSchedulers = new LateScheduler[numPartitions];
        this.cosManager = new COSManager(numPartitions, maxCOSSize, new ConflictDefinitionDefault());
        initLateSchedulers();
        initReplicaWorkers(numWorkers, numPartitions);
    }

    private void initReplicaWorkers(int numWorkers, int numPartitions) {
        statistics = new ThroughputStatistics(id, numWorkers, "results_" + id + ".txt", "");
        for (int i = 0; i < numWorkers; i++) {
            new HibridServiceReplicaWorker(i, i % numPartitions).start();
        }
    }

    private void initLateSchedulers() {
        for (int i = 0; i < this.lateSchedulers.length; i++) {
            this.lateSchedulers[i] = new LateScheduler(cosManager, queuesManager);
        }
    }

    protected void processOrderedRequest(TOMMessage message) {
        var request = new Request().fromBytes(message.getContent());
        var requestId = request.getId();
        var commands = request.getCommands();
        context.put(requestId, new HibridRequestContext(commands.length, message));
        try {
            earlyScheduler.schedule(requestId, commands);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private class HibridServiceReplicaWorker extends Thread {
        private int thread_id;
        private int preferentialPartition;

        public HibridServiceReplicaWorker(int thread_id, int preferentialPartition) {
            super("HibridServiceReplicaWorker[" + id + "," + thread_id + "]");
            this.thread_id = thread_id;
            this.preferentialPartition = preferentialPartition;
        }

        public void run() {
            try {
                while (true) {
                    ServerCommand serverCommand = cosManager.getFrom(preferentialPartition);
                    boolean[] results = executor.execute(serverCommand.getCommand());
                    manageReply(serverCommand, results);
                    statistics.computeStatistics(thread_id, 1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        protected void manageReply(ServerCommand serverCommand, boolean[] results) {
            var requestId = serverCommand.getRequestId();
            var commandId = serverCommand.getCommandId();
            var ctx = context.get(requestId);
            ctx.add(new CommandResult(commandId, results));
            if (ctx.finished()) {
                var response = new Response(requestId, ctx.getResults());
                ctx.request.reply = new TOMMessage(id, ctx.request.getSession(), ctx.request.getSequence(), response.toBytes(), SVController.getCurrentViewId());
                replier.manageReply(ctx.request, null);
            }
        }
    }
}
