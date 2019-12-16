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
        for (Command command : commands) {
            addToQueues(requestId, command);
        }
    }

    private void addToQueues(int requestId, Command command) throws InterruptedException {
        var serverCommand = new ServerCommand(requestId, command);
        Stats.earlySchedulerInit(serverCommand);
        for (int partition : serverCommand.distinctPartitions) {
            queuesManager.putCommandIn(partition, serverCommand);
        }
        Stats.earlySchedulerEnd(serverCommand);
    }

}
