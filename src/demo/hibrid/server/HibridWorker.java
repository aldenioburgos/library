/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.server;

import demo.hibrid.server.graph.COSManager;
import demo.hibrid.stats.Stats;

/**
 * @author aldenio
 */
public class HibridWorker extends Thread {
    private int preferentialPartition;
    private COSManager cosManager;
    private HibridExecutor executor;
    private HibridReplier hibridReplier;
    private int thread_id;

    public HibridWorker(int id, int thread_id, int preferentialPartition, COSManager cosManager, HibridExecutor executor, HibridReplier hibridReplier) {
        super("HibridServiceReplicaWorker[" + id + ", " + thread_id + "]");
        this.preferentialPartition = preferentialPartition;
        this.cosManager = cosManager;
        this.executor = executor;
        this.hibridReplier = hibridReplier;
        this.thread_id = thread_id;
    }

    public void run() {
        try {
            while (true) {
                var workerInit = System.nanoTime();
                ServerCommand serverCommand = cosManager.get(preferentialPartition);

                Stats.replicaWorkerInit(thread_id, workerInit, serverCommand);
                boolean[] results = executor.execute(serverCommand.command);
                Stats.replicaWorkerEnd(thread_id, serverCommand);

                cosManager.remove(serverCommand);
                Stats.commandRemoved(thread_id, serverCommand);

                hibridReplier.manageReply(serverCommand, results);
                Stats.replySent(thread_id, serverCommand);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}

