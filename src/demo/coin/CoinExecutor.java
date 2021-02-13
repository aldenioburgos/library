package demo.coin;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.SingleExecutable;

public class CoinExecutor implements SingleExecutable {

    private CoinGlobalState globalState = new CoinGlobalState();

    @Override
    public byte[] executeOrdered(byte[] bytes, MessageContext messageContext) {
        CoinOperation op = CoinOperation.loadFrom(bytes);
        op.execute(globalState);
        return new byte[]{1};
    }


    @Override
    public byte[] executeUnordered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException();
    }
}
