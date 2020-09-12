package demo.hibrid.server.queue;

import demo.hibrid.server.graph.LockFreeNode;
import demo.hibrid.stats.Stats;

import java.util.Arrays;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

public class QueuesManager {

    public final TransferQueue<LockFreeNode>[] queues;

    public QueuesManager(int numQueues) {
        Stats.partitions = numQueues;
        this.queues = new TransferQueue[numQueues];
        for (int i = 0; i < queues.length; i++) {
            queues[i] = new LinkedTransferQueue<>();
        }
    }


    public int size() {
        int size = 0;
        for (int i = 0; i < queues.length; i++) {
            size += queues[i].size();
        }
        return size;
    }

    @Override
    public String toString() {
        return "QueuesManager{queues=" + Arrays.toString(queues) + '}';
    }

}
