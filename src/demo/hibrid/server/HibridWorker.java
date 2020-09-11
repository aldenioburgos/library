package demo.hibrid.server;

import demo.hibrid.server.graph.COSManager;
import demo.hibrid.server.graph.LockFreeNode;

/**
 * @author aldenio
 */
public class HibridWorker extends Thread {
    private final int preferentialPartition;
    private final COSManager cosManager;
    private final ExecutorInterface executor;
    private final HibridReplier hibridReplier;
    private final int workerId;

    public HibridWorker(int workerId,
                        int preferentialPartition,
                        COSManager cosManager,
                        ExecutorInterface executor,
                        HibridReplier hibridReplier) {
        super("HibridiWorker[" + workerId + "]");
        this.preferentialPartition = preferentialPartition;
        this.cosManager = cosManager;
        this.executor = executor;
        this.hibridReplier = hibridReplier;
        this.workerId = workerId;
    }

    public void run() {
        try {
            while (true) {
                CommandEnvelope commandEnvelope = getNextAvailableCommand();
                boolean[] results = executor.execute(commandEnvelope.command);
                remove(commandEnvelope);
                hibridReplier.manageReply(commandEnvelope, results);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(workerId);
        }
    }

    private CommandEnvelope getNextAvailableCommand() {
        try {
            return cosManager.readyQueue.take().commandEnvelope;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void remove(CommandEnvelope commandEnvelope) {
        commandEnvelope.atomicNode.get().status.compareAndSet(LockFreeNode.RESERVED, LockFreeNode.REMOVED);
    }


    @Override
    public String toString() {
        return "HibridWorker{" +
                "preferentialPartition=" + preferentialPartition +
                ", workerId=" + workerId +
                '}';
    }
}

