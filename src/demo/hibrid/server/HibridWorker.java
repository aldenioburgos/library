/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.server;

import demo.hibrid.server.graph.COSManager;

/**
 * @author aldenio
 */
public class HibridWorker extends Thread {
    private final int preferentialPartition;
    private final COSManager cosManager;
    private final HibridExecutor executor;
    private final HibridReplier hibridReplier;
    private final int workerId;

    public HibridWorker(int id,
                        int workerId,
                        int preferentialPartition,
                        COSManager cosManager,
                        HibridExecutor executor,
                        HibridReplier hibridReplier) {
        super("HibridServiceReplicaWorker[" + id + ", " + workerId + "]");
        this.preferentialPartition = preferentialPartition;
        this.cosManager = cosManager;
        this.executor = executor;
        this.hibridReplier = hibridReplier;
        this.workerId = workerId;
    }

    public void run() {
        while (true) {
//            Stats.log(new Event(REPLICA_WORKER_WILL_TAKE_COMMAND, null, null, null, workerId));
            CommandEnvelope commandEnvelope = cosManager.getFrom(preferentialPartition);

//            Stats.log(new Event(REPLICA_WORKER_STARTED, commandEnvelope.requestId, commandEnvelope.command.id, null, workerId));
            boolean[] results = executor.execute(commandEnvelope.command);
//            Stats.log(new Event(REPLICA_WORKER_ENDED, commandEnvelope.requestId, commandEnvelope.command.id, null, workerId));
            cosManager.remove(commandEnvelope);
//            Stats.log(new Event(COMMAND_REMOVED, commandEnvelope.requestId, commandEnvelope.command.id, null, workerId));
            hibridReplier.manageReply(commandEnvelope, results);
//            Stats.log(new Event(COMMAND_REPLIED, commandEnvelope.requestId, commandEnvelope.command.id, null, workerId));
        }
    }

    @Override
    public String toString() {
        return "HibridWorker{" +
                "preferentialPartition=" + preferentialPartition +
                ", workerId=" + workerId +
                '}';
    }
}

