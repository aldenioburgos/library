package demo.hibrid.server.scheduler;

import demo.hibrid.server.ServerCommand;
import demo.hibrid.server.graph.COSManager;
import demo.hibrid.stats.Event;
import demo.hibrid.stats.Stats;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;

import static demo.hibrid.stats.EventType.*;

public class LateScheduler extends Thread {

    private final int id;
    private final  COSManager cosManager;
    private final BlockingQueue<ServerCommand> queue;


    public LateScheduler(int id, BlockingQueue<ServerCommand> queue, COSManager cosManager) {
        super("LateScheduler[" + id + "]");
        this.id = id;
        this.queue = queue;
        this.cosManager = cosManager;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Stats.log(new Event(LATE_SCHEDULER_WILL_TAKE_COMMAND, null, null, id, null));
                var command = queue.take();
                schedule(command);
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }

    public void schedule(ServerCommand serverCommand) throws BrokenBarrierException, InterruptedException {
        Stats.log(new Event(LATE_SCHEDULER_STARTED, serverCommand.requestId, serverCommand.command.id, id, null));
        if (serverCommand.hasBarrier()) {
            serverCommand.barrier.await();
        }

        if (this.id == serverCommand.distinctPartitions[0]) {
            cosManager.addTo(id, serverCommand);
        }

        if (serverCommand.hasBarrier()) {
            serverCommand.barrier.await();
        }
        Stats.log(new Event(LATE_SCHEDULER_ENDED, serverCommand.requestId, serverCommand.command.id, id, null));
    }

}
