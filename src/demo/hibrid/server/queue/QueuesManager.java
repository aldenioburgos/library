package demo.hibrid.server.queue;

import demo.hibrid.server.CommandEnvelope;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class QueuesManager {

    private final BlockingQueue<CommandEnvelope>[] queues;

    public QueuesManager(int numQueues, int maxQueueSize) {
        this.queues = new BlockingQueue[numQueues];
        for (int i = 0; i < queues.length; i++) {
            queues[i] = new ArrayBlockingQueue<>(maxQueueSize);
        }
    }

    public void putCommandIn(int partition, CommandEnvelope commandEnvelope) throws InterruptedException {
        queues[partition].put(commandEnvelope);
    }

    public BlockingQueue<CommandEnvelope> getQueue(int partition) {
        return queues[partition];
    }

    @Override
    public String toString() {
        return "QueuesManager{" +
                "sizes=" + Arrays.toString(Arrays.stream(queues).mapToInt(Collection::size).toArray()) +
                "queues=" + Arrays.toString(queues) +
                '}';
    }

}
