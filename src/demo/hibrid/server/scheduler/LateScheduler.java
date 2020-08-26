package demo.hibrid.server.scheduler;

import demo.hibrid.server.CommandEnvelope;
import demo.hibrid.server.graph.COS;
import demo.hibrid.stats.Event;
import demo.hibrid.stats.Stats;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;

import static demo.hibrid.stats.EventType.*;

public class LateScheduler extends Thread {

    private final int id;
    private final  COS cos;
    private final BlockingQueue<CommandEnvelope> queue;


    public LateScheduler(int id, BlockingQueue<CommandEnvelope> queue, COS cos) {
        super("LateScheduler[" + id + "]");
        this.id = id;
        this.queue = queue;
        this.cos = cos;
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

    public void schedule(CommandEnvelope commandEnvelope) throws BrokenBarrierException, InterruptedException {
        Stats.log(new Event(LATE_SCHEDULER_STARTED, commandEnvelope.requestId, commandEnvelope.command.id, id, null));
        if (this.id == commandEnvelope.distinctPartitions[0]) {
            cos.createNodeFor(commandEnvelope);
        }

        if (commandEnvelope.hasBarrier()) {
            commandEnvelope.barrier.await();
        }

        if (this.id == commandEnvelope.distinctPartitions[0]) {
            cos.excludeRemovedNodes_insertDependencies_insertNewNode(commandEnvelope);
        } else {
            cos.insertDependencies(commandEnvelope);
        }

        if (commandEnvelope.hasBarrier()) {
            commandEnvelope.barrier.await();
        }
        Stats.log(new Event(LATE_SCHEDULER_ENDED, commandEnvelope.requestId, commandEnvelope.command.id, id, null));
    }
}
