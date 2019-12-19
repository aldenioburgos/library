package demo.hibrid.server.queue;

import demo.hibrid.server.ServerCommand;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class QueuesManager {

    private BlockingQueue<ServerCommand>[] queues;

    public QueuesManager(int numQueues, int maxQueueSize) {
        this.queues = new BlockingQueue[numQueues];
        for (int i = 0; i < queues.length; i++) {
            queues[i] = new ArrayBlockingQueue<>(maxQueueSize);
        }
    }

    public void putCommandIn(int partition, ServerCommand serverCommand) throws InterruptedException {
        queues[partition].put(serverCommand);
    }

    public ServerCommand takeCommandFrom(int partition) throws InterruptedException {
        return queues[partition].take();
    }

    @Override
    public String toString() {
        return "QueuesManager{" +
                "sizes=" + Arrays.toString(Arrays.stream(queues).mapToInt(Collection::size).toArray()) +
                "queues=" + Arrays.toString(queues) +
                '}';
    }

    public int getNumQueues() {
        return this.queues.length;
    }
}
