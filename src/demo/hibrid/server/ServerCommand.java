package demo.hibrid.server;

import demo.hibrid.request.Command;
import demo.hibrid.server.graph.LockFreeNode;

import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;

public class ServerCommand {

    public final int requestId;
    public final CyclicBarrier barrier;
    private Command command;
    private LockFreeNode<ServerCommand> node;

    public final int[] partitions;
    public final int[] distinctPartitions;

    public ServerCommand(int requestId, Command command) {
        this.requestId = requestId;
        this.command = command;
        this.partitions = command.getPartitions();
        this.distinctPartitions = Arrays.stream(this.partitions).distinct().toArray();
        if (distinctPartitions.length > 1) {
            this.barrier = new CyclicBarrier(distinctPartitions.length);
        } else {
            barrier = null;
        }
    }


    public Command getCommand() {
        return command;
    }

    public int getCommandId() {
        return command.getId();
    }

    public void setNode(LockFreeNode<ServerCommand> node) {
        this.node = node;
    }

    public LockFreeNode<ServerCommand> getNode() {
        return node;
    }


    @Override
    public String toString() {
        return "ServerCommand{" +
                "requestId=" + requestId +
                ", barrier=" + ((barrier == null) ? " null" : barrier.getNumberWaiting() + " de " + barrier.getParties()) +
                ", " + command +
                '}';
    }
}
