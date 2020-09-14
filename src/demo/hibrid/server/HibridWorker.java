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
                LockFreeNode newNode = cosManager.readyQueue.take();
                boolean[] results = executor.execute(newNode.command);
                markCompleted(newNode);
                hibridReplier.manageReply(newNode, results);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(workerId);
        }
    }


    private void markCompleted(LockFreeNode lockFreeNode) {
        lockFreeNode.completed =  true;
        try {
            lockFreeNode.writeLock.lock();
            notifyListeners(lockFreeNode);
        } finally {
            lockFreeNode.writeLock.unlock();
        }
        cosManager.releaseSpace();

    }

    private void notifyListeners(LockFreeNode currentNode) {
        for (var listenersHead : currentNode.listeners) {
            listenersHead.forEach(listenerNode -> {
                listenerNode.dependencies.decrement();
                if (listenerNode.isReady()) {
                    cosManager.addToReadyQueue(listenerNode);
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

