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
//        Stats.log(new Event(EARLY_SCHEDULER_STARTED, requestId, command.id, null, null));

        var commandEnvelope = new CommandEnvelope(requestId, command);
        for (int partition : commandEnvelope.distinctPartitions) {
            try {
//                System.out.println("Early Scheduler put "+commandEnvelope+" in partition "+partition);
                queuesManager.putCommandIn(partition, commandEnvelope);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

//        Stats.log(new Event(EARLY_SCHEDULER_ENDED, requestId, command.id, null, null));
    }

    @Override
    public String toString() {
        return "EarlyScheduler{" +
                "queuesManager=" + queuesManager +
                '}';
    }
}
