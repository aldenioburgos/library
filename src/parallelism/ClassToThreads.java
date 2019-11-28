/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parallelism;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;

/**
 * @author eduardo
 */
public class ClassToThreads {

    public static final int CONC = 0; // concurrent
    public static final int SYNC = 1; //synchronized
    public int type;
    public int[] tIds;
    public int classId;

    private int executorIndex = 0;

    private BlockingQueue[] queues;
    public CyclicBarrier barrier;

    public ClassToThreads(int classId, int type, int[] ids) {
        this.classId = classId;
        this.type = type;
        this.tIds = ids;
    }

    public void setQueues(BlockingQueue[] q) {
        if (q.length != tIds.length) {
            System.err.println("INCORRECT MAPPING");
        }
        this.queues = q;
        if (this.type == SYNC) {
            this.barrier = new CyclicBarrier(tIds.length);
        }
    }

    public void putInNextQueue(Object request) throws InterruptedException {
        queues[executorIndex].put(request);
        executorIndex = (executorIndex + 1) % queues.length;
    }

    public void putInAllQueues(MessageContextPair request) throws InterruptedException {
        for (BlockingQueue q : queues) {
            q.put(request);
        }
    }
}
