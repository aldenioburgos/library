package demo.hibrid.server.scheduler;

import demo.hibrid.server.graph.COSManager;
import demo.hibrid.server.graph.LockFreeNode;
import demo.hibrid.server.queue.QueuesManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TransferQueue;

/**
 * @author aldenio
 */
public class LateScheduler extends Thread {

    private final int id;
    private final int numPartitions;
    private final COSManager cosManager;
    private final TransferQueue<LockFreeNode> queue;
    private final List<LockFreeNode> myNodes;
    private final List<LockFreeNode> otherNodes;

    public LateScheduler(int id, int numPartitions, QueuesManager queuesManager, COSManager cosManager) {
        super("LateScheduler[" + id + "]");
        this.id = id;
        this.myNodes = new LinkedList<>();
        this.otherNodes = new LinkedList<>();
        this.queue = queuesManager.queues[id];
        this.numPartitions = numPartitions;
        this.cosManager = cosManager;
    }



    @Override
    public void run() {
        try {
            while (true) {
                List<LockFreeNode> commands = new ArrayList<>();
                commands.add(queue.take());
                queue.drainTo(commands);
                commands.forEach(this::schedule);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(id);
        }
    }

    private void schedule(LockFreeNode newNode) {
        boolean chegueiPrimeiro = newNode.created.compareAndSet(false, true);

        removeCompletedAndInsertDependencies(myNodes, newNode);
        removeCompletedAndInsertDependencies(otherNodes, newNode);

        if (chegueiPrimeiro) {
            myNodes.add(newNode);
        } else {
            otherNodes.add(newNode);
        }

        if (newNode.atomicCounter.decrementAndGet() == 0) {
            if (newNode.inserted.compareAndSet(false, true)) {
                if (newNode.isReady()) {
                    cosManager.readyQueue.add(newNode);
                }
            } else {
                throw new IllegalStateException("AtomicCounter == 0 and inserted == true");
            }
        }
    }


    private void removeCompletedAndInsertDependencies(List<LockFreeNode> nodes, LockFreeNode node) {
        Iterator<LockFreeNode> myIterator = nodes.iterator();
        while (myIterator.hasNext()) {
            LockFreeNode myNode = myIterator.next();
            if (myNode.completed.get()) {
                myIterator.remove();
            } else if (cosManager.isDependent(node, myNode)) {
                insertDependentNode(myNode, node);
            }
        }
    }

    private void insertDependentNode(LockFreeNode oldNode, LockFreeNode newNode) {
        try {
            oldNode.readLock.lock();
            if (!oldNode.completed.get()) {
                oldNode.listeners[id].insert(newNode);
                newNode.dependencies.increment();
            }
        } finally {
            oldNode.readLock.unlock();
        }

    }


    @Override
    public String toString() {
        return "LateScheduler{" +
                "id=" + id +
                ", queue=" + queue +
                ", numPartitions=" + numPartitions +
                ", myNodes=" + myNodes +
                ", otherNodes=" + otherNodes +
                '}';
    }
}
