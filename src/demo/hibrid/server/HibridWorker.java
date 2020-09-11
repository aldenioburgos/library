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
                CommandEnvelope commandEnvelope = cosManager.readyQueue.take().commandEnvelope;
                boolean[] results = new boolean[]{true, true};//executor.execute(commandEnvelope.command);
                markCompleted(commandEnvelope.getNode());
                hibridReplier.manageReply(commandEnvelope, results);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(workerId);
        }
    }


    private void markCompleted(LockFreeNode lockFreeNode) {
        if (lockFreeNode.completed.compareAndSet(false, true)) {
//            try {
//                lockFreeNode.writeLock.lock();
//                //notifyListeners(lockFreeNode);
//            } finally {
//                lockFreeNode.writeLock.unlock();
//            }
            cosManager.releaseSpace();
        }
    }

    private void notifyListeners(LockFreeNode currentNode) {
        for (var listenersHead : currentNode.listeners) {
            listenersHead.forEach(node -> {
                node.dependencies.decrement();
                if (node.isReady()) {
                    cosManager.addToReadyQueue(node);
                }
            });
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

