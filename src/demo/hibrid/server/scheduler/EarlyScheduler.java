package demo.hibrid.server.scheduler;

import demo.hibrid.request.Command;
import demo.hibrid.server.ServerCommand;
import demo.hibrid.server.queue.QueuesManager;
import demo.hibrid.stats.Stats;

public class EarlyScheduler {

    private QueuesManager queuesManager;

    public EarlyScheduler(QueuesManager queuesManager) {
        assert queuesManager != null : "Invalid Argument, queuesManager == null.";
        this.queuesManager = queuesManager;
    }

    public void schedule(int requestId, Command[] commands) {
        assert commands.length > 0 : "Invalid Argument, commands está vazio.";
        try {
            for (Command command : commands) {
                addToQueues(requestId, command);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void addToQueues(int requestId, Command command) throws InterruptedException {
        assert command != null: "Invalid Argument, command == null";

        var serverCommand = new ServerCommand(requestId, command);
        Stats.earlySchedulerInit(serverCommand);
        for (int partition : serverCommand.distinctPartitions) {
            queuesManager.putCommandIn(partition, serverCommand);
        }
        Stats.earlySchedulerEnd(serverCommand);
    }

}
