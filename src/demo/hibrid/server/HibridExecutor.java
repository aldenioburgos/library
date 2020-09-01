package demo.hibrid.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.SingleExecutable;
import demo.hibrid.request.Command;

import java.util.Random;

public class HibridExecutor implements SingleExecutable {

    private final int minReadTime;
    private final int maxReadTime;
    private final int minWriteTime;
    private final int maxWriteTime;
    private final Random rand = new Random();

    public HibridExecutor(int minTimeToExecuteRead,
                          int maxTimeToExecuteRead,
                          int minTimeToExecuteWrite,
                          int maxTimeToExecuteWrite) {
        assert minTimeToExecuteRead >= 0 : "Invalid negative argument minTimeToExecuteRead!";
        assert maxTimeToExecuteRead >= 0 : "Invalid negative argument maxTimeToExecuteRead!";
        assert minTimeToExecuteWrite >= 0 : "Invalid negative argument minTimeToExecuteWrite!";
        assert maxTimeToExecuteWrite >= 0 : "Invalid negative argument maxTimeToExecuteWrite!";

        this.minReadTime = minTimeToExecuteRead;
        this.maxReadTime = maxTimeToExecuteRead;
        this.minWriteTime = minTimeToExecuteWrite;
        this.maxWriteTime = maxTimeToExecuteWrite;
    }

    public boolean[] execute(Command command) {
        long endTime = System.currentTimeMillis() + ((command.type == Command.ADD) ? minWriteTime + rand.nextInt(maxWriteTime) : minReadTime + rand.nextInt(maxReadTime));
        var u = 0;
        while (System.currentTimeMillis() < endTime) {
            u++; // Active waiting
        }
        return new boolean[command.indexes.length];
    }

    @Override
    public byte[] executeOrdered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] executeUnordered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "HibridExecutor{" +
                "minReadTime=" + minReadTime +
                ", maxReadTime=" + maxReadTime +
                ", minWriteTime=" + minWriteTime +
                ", maxWriteTime=" + maxWriteTime +
                ", rand=" + rand +
                '}';
    }
}
