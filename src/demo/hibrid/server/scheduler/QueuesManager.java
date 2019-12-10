package demo.hibrid.server.scheduler;

import demo.hibrid.server.ServerCommand;

import java.util.Arrays;
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

    ServerCommand takeCommandFrom(int partition) throws InterruptedException {
        var x = queues[partition].take();
        System.out.println(this);
        return x;
    }

    void putCommandIn(int partition, ServerCommand serverCommand) throws InterruptedException {
        queues[partition].put(serverCommand);
        System.out.println(this);
    }

    @Override
    public String toString() {
        return "QueuesManager{" +
                "queues=" + Arrays.toString(queues) +
                '}';
    }
}
