package demo.hibrid.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.SingleExecutable;
import demo.hibrid.request.Command;

import java.util.LinkedList;
import java.util.List;

public class HibridExecutor implements SingleExecutable {

    private final List<Integer> theList = new LinkedList<>();


    public HibridExecutor(int maxIndex) {
        for (int i = 0; i < maxIndex; i++) {
            this.theList.add(i);
        }
    }

    public void execute(Command[] command, MessageContext messageContext) {

    }

    public void execute(Command command, MessageContext messageContext) {

    }

    @Override
    public byte[] executeOrdered(byte[] bytes, MessageContext messageContext) {
        return new byte[0];
    }

    @Override
    public byte[] executeUnordered(byte[] bytes, MessageContext messageContext) {
        return new byte[0];
    }
}
