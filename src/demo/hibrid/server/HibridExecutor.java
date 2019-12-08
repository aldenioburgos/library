package demo.hibrid.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.SingleExecutable;
import demo.hibrid.request.Command;

public class HibridExecutor implements SingleExecutable {

    private final int  MIN_TIME_TO_EXECUTE_READ;
    private final int  MIN_TIME_TO_EXECUTE_WRITE;

    public HibridExecutor(int minTimeToExecuteRead, int minTimeToExecuteWrite) {
        this.MIN_TIME_TO_EXECUTE_READ = minTimeToExecuteRead;
        this.MIN_TIME_TO_EXECUTE_WRITE = minTimeToExecuteWrite;
    }

    @Override
    public byte[] executeOrdered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] executeUnordered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException();
    }


    public boolean[] execute(Command command) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + ((command.getType() == Command.ADD)? MIN_TIME_TO_EXECUTE_WRITE: MIN_TIME_TO_EXECUTE_READ);
        while (System.currentTimeMillis() < endTime) {
            // Active waiting
        }
        return new boolean[command.getIndexes().length];
    }
}
