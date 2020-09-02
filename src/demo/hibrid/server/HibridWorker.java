/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.server;

import demo.hibrid.server.graph.COSManager;

import java.util.concurrent.BrokenBarrierException;

/**
 * @author aldenio
 */
public class HibridWorker extends Thread {
    private final int preferentialPartition;
    private final COSManager cosManager;
    private final ExecutorInterface executor;
    private final HibridReplier hibridReplier;
    private final int workerId;

    public HibridWorker(int id,
                        int workerId,
                        int preferentialPartition,
                        COSManager cosManager,
                        ExecutorInterface executor,
                        HibridReplier hibridReplier) {
        super("HibridServiceReplicaWorker[" + id + ", " + workerId + "]");
        this.preferentialPartition = preferentialPartition;
        this.cosManager = cosManager;
        this.executor = executor;
        this.hibridReplier = hibridReplier;
        this.workerId = workerId;
    }

    public void run() {
        try {
            while (true) {
                CommandEnvelope commandEnvelope = cosManager.getFrom(preferentialPartition);
                boolean[] results = executor.execute(commandEnvelope.command);
                cosManager.remove(commandEnvelope);
                hibridReplier.manageReply(commandEnvelope, results);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(workerId);
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

