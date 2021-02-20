package demo.coin;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.SingleExecutable;
import demo.coin.core.CoinGlobalState;
import demo.coin.core.CoinOperation;

public class CoinExecutor implements SingleExecutable {

    private CoinGlobalState globalState = new CoinGlobalState();

    @Override
    public byte[] executeOrdered(byte[] bytes, MessageContext messageContext) {
        try {
            CoinOperation op = CoinOperation.loadFrom(bytes);
            op.execute(globalState);
            return new byte[]{1};
        } catch (RuntimeException e) {
            return new byte[]{0};
        }
    }


    @Override
    public byte[] executeUnordered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException();
    }
}
