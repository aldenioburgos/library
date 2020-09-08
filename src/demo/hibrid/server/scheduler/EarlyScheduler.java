package demo.hibrid.server.scheduler;

import demo.hibrid.request.Command;
import demo.hibrid.server.CommandEnvelope;
import demo.hibrid.server.graph.COSManager;
import demo.hibrid.server.queue.QueuesManager;

public class EarlyScheduler {

    private final QueuesManager queuesManager;
    private final COSManager cosManager;

    public EarlyScheduler(QueuesManager queuesManager, COSManager cosManager) {
        assert queuesManager != null : "Invalid Argument, queuesManager == null.";
        this.queuesManager = queuesManager;
        this.cosManager = cosManager;
    }

    public void schedule(int requestId, Command[] commands) {
        assert commands.length > 0 : "Invalid Argument, commands está vazio.";
        for (Command command : commands) {
            cosManager.acquireSpace();
            addToQueues(requestId, command);
        }
    }

    private void addToQueues(int requestId, Command command) {
        assert command != null : "Invalid Argument, command == null";
        try {
            var commandEnvelope = new CommandEnvelope(requestId, command);
            for (int partition : commandEnvelope.distinctPartitions) {
                queuesManager.putCommandIn(partition, commandEnvelope);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "EarlyScheduler{" +
                "queuesManager=" + queuesManager +
                '}';
    }
}
