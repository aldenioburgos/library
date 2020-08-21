/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.server;

import demo.hibrid.server.graph.COSManager;
import demo.hibrid.stats.Event;
import demo.hibrid.stats.Stats;

import static demo.hibrid.stats.EventType.*;

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
            Stats.log(new Event(REPLICA_WORKER_WILL_TAKE_COMMAND, null, null, null, workerId));
            ServerCommand serverCommand = cosManager.get(preferentialPartition);

            Stats.log(new Event(REPLICA_WORKER_STARTED, serverCommand.requestId, serverCommand.command.id, null, workerId));
            boolean[] results = executor.execute(serverCommand.command);
            Stats.log(new Event(REPLICA_WORKER_ENDED, serverCommand.requestId, serverCommand.command.id, null, workerId));

            cosManager.remove(serverCommand);
            Stats.log(new Event(COMMAND_REMOVED, serverCommand.requestId, serverCommand.command.id, null, workerId));

            hibridReplier.manageReply(serverCommand, results);
            Stats.log(new Event(COMMAND_REPLIED, serverCommand.requestId, serverCommand.command.id, null, workerId));
        }
    }


}

