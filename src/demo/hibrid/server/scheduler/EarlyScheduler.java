package demo.hibrid.server.scheduler;

import demo.hibrid.request.Command;
import demo.hibrid.server.CommandEnvelope;
import demo.hibrid.server.queue.QueuesManager;
import demo.hibrid.stats.Event;
import demo.hibrid.stats.Stats;

import static demo.hibrid.stats.EventType.EARLY_SCHEDULER_ENDED;
import static demo.hibrid.stats.EventType.EARLY_SCHEDULER_STARTED;

public class EarlyScheduler {

    private final QueuesManager queuesManager;

    public EarlyScheduler(QueuesManager queuesManager) {
        assert queuesManager != null : "Invalid Argument, queuesManager == null.";
        this.queuesManager = queuesManager;
    }

    public void schedule(int requestId, Command[] commands) {
        assert commands.length > 0 : "Invalid Argument, commands está vazio.";
        for (Command command : commands) {
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
