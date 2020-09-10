package demo.hibrid.server.queue;

import demo.hibrid.server.CommandEnvelope;
import demo.hibrid.stats.Stats;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class QueuesManager {

    public final BlockingQueue<CommandEnvelope>[] queues;

    public QueuesManager(int numQueues, int maxQueueSize) {
        Stats.partitions = numQueues;
        this.queues = new BlockingQueue[numQueues];
        for (int i = 0; i < queues.length; i++) {
            queues[i] = new ArrayBlockingQueue<>(maxQueueSize);
        }
    }

    public void putCommandIn(int partition, CommandEnvelope commandEnvelope) throws InterruptedException {
        queues[partition].put(commandEnvelope);
    }


    @Override
    public String toString() {
        return "QueuesManager{" +
                "queue[0]=" + queues[0].peek() +
                "queue[1]=" + queues[1].peek() +
                '}';
    }

}
