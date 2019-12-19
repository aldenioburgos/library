package demo.hibrid.server.scheduler;

import demo.hibrid.server.ServerCommand;
import demo.hibrid.server.graph.COSManager;
import demo.hibrid.server.queue.QueuesManager;
import demo.hibrid.stats.Stats;

import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;

public class LateScheduler extends Thread {

    private COSManager cosManager;
    private QueuesManager queuesManager;
    private int myQueue;
    private int myCos;
    private int id;

    public LateScheduler(COSManager cosManager, QueuesManager queuesManager, int id, int myQueue, int myCos) {
        super("LateScheduler[" + id + "]");
        this.cosManager = cosManager;
        this.queuesManager = queuesManager;
        this.myCos = myCos;
        this.myQueue = myQueue;
        this.id = id;
    }

    public void schedule(ServerCommand command) throws BrokenBarrierException, InterruptedException {
        if (command.barrier != null) {
            command.barrier.await();
        }
        if (myCos == min(command.distinctPartitions)) {
            cosManager.addTo(myCos, command);
        }
        if (command.barrier != null) {
            command.barrier.await();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                var takeInit = System.nanoTime();
                var command = queuesManager.takeCommandFrom(myQueue);
                Stats.lateSchedulerInit(id, takeInit, command);
                schedule(command);
                Stats.lateSchedulerEnd(id, command);
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }


    private int min(int[] commandPartitions) {
        assert commandPartitions.length > 0 : "Todo comando deve acessar ao menos uma partição.";
        return Arrays.stream(commandPartitions).min().getAsInt();
    }

}
