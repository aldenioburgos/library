package demo.coin;

import bftsmart.tom.core.messages.TOMMessage;
import demo.coin.core.transactions.CoinOperation;
import parallelism.MessageContextPair;
import parallelism.MultiOperationCtx;
import parallelism.hibrid.late.HibridLockFreeNode;

/**
 *
 * @author eduardo
 */
public class CoinMessageContextPair extends MessageContextPair {
    public TOMMessage request;
    public int classId;
    public CoinOperation operation;
    public int index;
    public byte[] resp;
    public short opId;
    public MultiOperationCtx ctx;
    public HibridLockFreeNode node = null;
    public int threadId;

    public CoinMessageContextPair(TOMMessage message, int classId, int index, short operation, short opId, MultiOperationCtx ctx) {
        super(message, classId, index, operation, opId, ctx);
        throw new UnsupportedOperationException("Marreta pra funcionar com o código do prof mexendo o mínimo possível nele.");
    }

    public CoinMessageContextPair(TOMMessage message, int classId, int index, CoinOperation operation, short opId, MultiOperationCtx ctx) {
        super(message, classId,  index, (short)0, opId, ctx);
        this.request = message;
        this.classId = classId;
        this.index = index;
        this.operation = operation;
        this.opId = opId;
        this.ctx = ctx;
    }

    @Override
    public String toString(){
        return "request [class id= "+classId+", operation id= "+opId+" requestID: "+request+ " index: "+index+"]";
    }

}
