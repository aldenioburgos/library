package demo.hibrid.server.scheduler;

import demo.hibrid.server.CommandEnvelope;
import demo.hibrid.server.graph.COS;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;

import static demo.hibrid.server.graph.LockFreeNode.INSERTED;
import static demo.hibrid.server.graph.LockFreeNode.NEW;

public class LateScheduler extends Thread {

    private final int id;
    private final BlockingQueue<CommandEnvelope> queue;
    private final COS cos;


    public LateScheduler(int id, BlockingQueue<CommandEnvelope> queue, COS cos) {
        super("LateScheduler[" + id + "]");
        this.id = id;
        this.queue = queue;
        this.cos = cos;

        assert cos.id == this.id : "Deram o COS["+cos.id+"] para o lateScheduler["+id+"]";
    }

    @Override
    public void run() {
        try {
            while (true) {
                var commands = new ArrayList<CommandEnvelope>(queue.size());
                queue.drainTo(commands);
                for (var command : commands) {
                    schedule(command);
                }
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }

    public void schedule(CommandEnvelope commandEnvelope) throws BrokenBarrierException, InterruptedException {
//        Stats.log(new Event(LATE_SCHEDULER_STARTED, commandEnvelope.requestId, commandEnvelope.command.id, id, null));
        if (this.id == commandEnvelope.distinctPartitions[0]) {
            cos.createNodeFor(commandEnvelope);
        }
        if (commandEnvelope.hasBarrier()) {
            commandEnvelope.barrier.await();
        }

        if (this.id == commandEnvelope.distinctPartitions[0]) {
            cos.cleanRemovedNodesInsertDependenciesAndInsertNewNode(commandEnvelope);
        } else {
            cos.excludeRemovedNodesInsertDependencies(commandEnvelope);
        }

        if (commandEnvelope.hasBarrier()) {
            commandEnvelope.barrier.await();
        }

        if (commandEnvelope.getNode().status.compareAndSet(NEW, INSERTED)) {
            commandEnvelope.getNode().testReady();
        }
//        Stats.log(new Event(LATE_SCHEDULER_ENDED, commandEnvelope.requestId, commandEnvelope.command.id, id, null));
    }

    @Override
    public String toString() {
        return "LateScheduler{" +
                "id=" + id +
                ", queue=" + queue +
                ", cos=" + cos.id +
                '}';
    }
}
