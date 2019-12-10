package demo.hibrid.server;

import demo.hibrid.request.Command;
import demo.hibrid.server.graph.COSNode;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ServerCommand {

    public final int requestId;
    private CyclicBarrier barrier;
    private Command command;
    private COSNode<ServerCommand> node;

    public ServerCommand(int requestId, Command command) {
        this.requestId = requestId;
        this.command = command;
        if (command.getPartitions().length > 1) {
            this.barrier = new CyclicBarrier(command.getPartitions().length);
        }
    }

    public void awaitAtCyclicBarrierIfNeeded() throws BrokenBarrierException, InterruptedException {
        if (barrier != null) {
            barrier.await();
        }
    }


    public Command getCommand() {
        return command;
    }

    public Integer getCommandId() {
        return command.getId();
    }

    public int[] getPartitions() {
        return command.getPartitions();
    }

    public void setNode(COSNode<ServerCommand> node) {
        this.node = node;
    }

    public COSNode<ServerCommand> getNode() {
        return node;
    }


    @Override
    public String toString() {
        return "ServerCommand{" +
                "requestId=" + requestId +
                ", barrier=" + barrier +
                ", command=" + command +
                ", node=" + node +
                '}';
    }
}
