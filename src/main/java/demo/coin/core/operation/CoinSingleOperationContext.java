package demo.coin.core.operation;

import bftsmart.tom.core.messages.TOMMessage;
import parallelism.hibrid.late.HibridLockFreeNode;

import java.util.Arrays;

public class CoinSingleOperationContext implements OperationContext{

    // preenchido pelo construtor
    public final int index;
    public final byte[] operation;
    public final CoinMultiOperationContext multiOperationCtx;

    // preenchido posteriormente
    public byte[] resp;
    public HibridLockFreeNode node = null;
    public int threadId;

    public CoinSingleOperationContext(CoinMultiOperationContext multiOperationContext, int index, byte[] operation) {
        this.index = index;
        this.operation = operation;
        this.multiOperationCtx = multiOperationContext;
    }

    public void setResponse(byte[] response) {
        this.resp = response;
        this.multiOperationCtx.add(index, resp);
    }


    public byte[] getResponseBytes() {
        return multiOperationCtx.multiOperationResponse.serialize();
    }

    public int getSequence() {
        return multiOperationCtx.multiOperationRequest.request.getSequence();
    }

    public int getSession() {
        return multiOperationCtx.multiOperationRequest.request.getSession();
    }

    public void setReply(TOMMessage tomMessage) {
        this.multiOperationCtx.multiOperationRequest.request.reply = tomMessage;
    }

    @Override
    public boolean isConcurrent() {
        return multiOperationCtx.isConcurrent();
    }

    public TOMMessage getTOMRequest() {
        return multiOperationCtx.getTOMRequest();
    }

    @Override
    public String toString() {
        return "CoinMessageContextPair{" +
                ", index=" + index +
                ", threadId=" + threadId +
                ", operation=" + Arrays.toString(operation) +
                ", resp=" + Arrays.toString(resp) +
                '}';
    }
}
