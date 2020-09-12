package demo.hibrid.server.scheduler;

import demo.hibrid.request.Command;
import demo.hibrid.server.graph.COSManager;
import demo.hibrid.server.graph.LockFreeNode;
import demo.hibrid.server.queue.QueuesManager;
import demo.hibrid.stats.Stats;

public class EarlyScheduler {

    private final QueuesManager queuesManager;
    private final COSManager cosManager;
    private final int numPartitions;

    public EarlyScheduler(QueuesManager queuesManager, COSManager cosManager, int numPartitions) {
        assert queuesManager != null : "Invalid Argument, queuesManager == null.";
        this.queuesManager = queuesManager;
        this.cosManager = cosManager;
        this.numPartitions = numPartitions;
    }

    public void schedule(int requestId, Command[] commands) {
        assert commands.length > 0 : "Invalid Argument, commands está vazio.";
        for (Command command : commands) {
            cosManager.acquireSpace();
            addToQueues(requestId, command);
        }
    }

    private void addToQueues(int requestId, Command command) {
        assert Stats.queueSize(queuesManager.size()) : "DEBUG";
        assert command != null : "Invalid Argument, command == null";
        try {
            var commandEnvelope = new LockFreeNode(requestId, command, numPartitions);
            for (int partition : commandEnvelope.distinctPartitions) {
                queuesManager.queues[partition].put(commandEnvelope);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "EarlyScheduler{queuesManager=" + queuesManager + '}';
    }
}
