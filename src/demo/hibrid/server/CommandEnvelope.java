package demo.hibrid.server;

import demo.hibrid.request.Command;
import demo.hibrid.server.graph.LockFreeNode;

import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CommandEnvelope {

    public final Command command;
    public final int requestId;
    public final int[] distinctPartitions;
    public final AtomicReference<LockFreeNode> atomicNode;
    public final AtomicInteger atomicCounter;

    public CommandEnvelope(int requestId, Command command) {
        this.requestId = requestId;
        this.command = command;
        this.distinctPartitions = command.distinctPartitions();
        this.atomicCounter = new AtomicInteger(distinctPartitions.length);
        this.atomicNode = new AtomicReference<>(null);
    }


    @Override
    public String toString() {
        return "{" +
                "request=" + requestId +
                ", insertionCounter=" + atomicCounter+
                ", distinctPartitions=" + Arrays.toString(distinctPartitions) +
                ", " + command +
                '}';
    }

}
