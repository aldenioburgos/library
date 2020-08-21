package demo.hibrid.server;

import demo.hibrid.request.Command;
import demo.hibrid.server.graph.LockFreeNode;

import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;

public class ServerCommand {

    public final int requestId;
    public final Command command;
    public final CyclicBarrier barrier;

    public final int[] distinctPartitions;
    private LockFreeNode<ServerCommand> node;

    public ServerCommand(int requestId, Command command) {
        this.requestId = requestId;
        this.command = command;
        this.distinctPartitions = Arrays.stream(command.partitions).distinct().toArray();
        if (distinctPartitions.length > 1) {
            this.barrier = new CyclicBarrier(distinctPartitions.length);
        } else {
            barrier = null;
        }
    }

    public void setNode(LockFreeNode<ServerCommand> node) {
        this.node = node;
    }

    public LockFreeNode<ServerCommand> getNode() {
        return node;
    }

    public boolean hasBarrier() {
        return barrier != null;
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
