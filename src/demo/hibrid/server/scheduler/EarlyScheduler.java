package demo.hibrid.server.scheduler;

import demo.hibrid.request.Command;
import demo.hibrid.server.ServerCommand;
import demo.hibrid.stats.Stats;

public class EarlyScheduler {

    private QueuesManager queuesManager;

    public EarlyScheduler(QueuesManager queuesManager) {
        this.queuesManager = queuesManager;
    }

    public void schedule(int requestId, Command[] commands) throws InterruptedException {
        for (int i = 0; i < commands.length; i++) {
            addToQueues(requestId, commands[i]);
        }
    }

    private void addToQueues(int requestId, Command command) throws InterruptedException {
        var serverCommand = new ServerCommand(requestId, command);
        Stats.earlySchedulerInit(serverCommand);
        var partitions = command.getPartitions();
        for (int i = 0; i < partitions.length; i++) {
            queuesManager.putCommandIn(partitions[i], serverCommand);
        }
        Stats.earlySchedulerEnd(serverCommand);
    }

}
