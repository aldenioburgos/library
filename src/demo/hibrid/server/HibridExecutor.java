package demo.hibrid.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.SingleExecutable;
import demo.hibrid.request.Command;

import java.util.LinkedList;
import java.util.List;

public class HibridExecutor implements SingleExecutable {

    private final List<Integer> dataList = new LinkedList<>();

    public HibridExecutor(int maxIndex) {
        for (int i = 0; i < maxIndex; i++) {
            this.dataList.add(i);
        }
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
        var indexes = command.getIndexes();
        var responses = new boolean[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            if (!dataList.contains(-indexes[i])) {
                responses[i] = false;
            } else {
                throw new RuntimeException("Não era para cair aqui, algo deu errado!");
            }
        }
        return responses;
    }
}
