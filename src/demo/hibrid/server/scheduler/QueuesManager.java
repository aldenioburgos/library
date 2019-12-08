package demo.hibrid.server.scheduler;

import demo.hibrid.server.ServerCommand;
import demo.hibrid.server.StatisticsCollector;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class QueuesManager {

    private BlockingQueue<ServerCommand>[] queues;

    public QueuesManager(int numQueues, int maxQueueSize) {
        this.queues =  new BlockingQueue[numQueues];
        for (int i = 0; i < queues.length; i++) {
            queues[i] = new ArrayBlockingQueue<>(maxQueueSize);
        }
    }

    public ServerCommand takeCommandFrom(int partition) throws InterruptedException{
        return queues[partition].take();
    }

    public void putCommandIn(int partition, ServerCommand serverCommand) throws InterruptedException {
        StatisticsCollector.getInstance().QueuesManagerPutCommandInStart.add(System.currentTimeMillis());
        queues[partition].put(serverCommand);
        StatisticsCollector.getInstance().QueuesManagerPutCommandInEnd.add(System.currentTimeMillis());
    }

}
