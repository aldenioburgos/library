package demo.hibrid.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.SingleExecutable;
import demo.hibrid.request.Command;

public interface ExecutorInterface extends SingleExecutable {


    @Override
    default byte[] executeOrdered(byte[] bytes, MessageContext messageContext){
        throw new UnsupportedOperationException();
    }

    @Override
    default byte[] executeUnordered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException();
    }

    boolean[] execute(Command command);
}
