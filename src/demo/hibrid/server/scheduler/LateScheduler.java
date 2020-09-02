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
                commands.add(queue.take());
                queue.drainTo(commands);
                for (var command : commands) {
                    schedule(command);
                }
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
            System.exit(id);
        }
    }

    public void schedule(CommandEnvelope commandEnvelope) throws BrokenBarrierException, InterruptedException {
        if (cos.createNodeFor(commandEnvelope)) {
            cos.cleanRemovedNodesInsertDependenciesAndInsertNewNode(commandEnvelope);
        } else {
            cos.excludeRemovedNodesInsertDependencies(commandEnvelope);
        }

        if (commandEnvelope.atomicCounter.decrementAndGet() == 0){
            if (commandEnvelope.atomicNode.get().status.compareAndSet(NEW, INSERTED)){
                commandEnvelope.atomicNode.get().testReady();
            } else {
                throw new IllegalStateException("AtomicCounter == 0 and status != NEW");
            }
        }
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
