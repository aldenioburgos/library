package demo.coin.late;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.SingleExecutable;
import demo.coin.core.CoinGlobalState;
import demo.coin.core.transactions.CoinOperation;

public class CoinExecutor implements SingleExecutable {

    private CoinGlobalState globalState;


    public CoinExecutor(CoinGlobalState globalState) {
        this.globalState = globalState;
    }

    @Override
    public byte[] executeOrdered(byte[] bytes, MessageContext messageContext) {
        try {
            CoinOperation op = CoinOperation.loadFrom(bytes);
            return op.execute(globalState);
        } catch (RuntimeException e) {
            return new byte[]{1};
        }
    }


    @Override
    public byte[] executeUnordered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException();
    }
}
