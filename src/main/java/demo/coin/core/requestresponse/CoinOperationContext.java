package demo.coin.core.requestresponse;

import bftsmart.tom.core.messages.TOMMessage;
import demo.coin.early.CoinHibridClassToThreads;
import parallelism.hibrid.late.HibridLockFreeNode;

public class CoinOperationContext implements OperationContext {

    // preenchido pelo construtor
    public final TOMMessage request;
    public final byte[] operation;
    public final CoinHibridClassToThreads cToT;

    // preenchido posteriormente
    public byte[] resp;
    public HibridLockFreeNode node;
    public int threadId;

    public CoinOperationContext(TOMMessage request, CoinHibridClassToThreads cToT) {
        this.cToT = cToT;
        this.request = request;
        this.operation = request.getContent();
    }

    public TOMMessage getTOMRequest() {
        return request;
    }

    @Override
    public int getClassId() {
        return request.groupId;
    }

    @Override
    public boolean isConcurrent() {
        return cToT.isConcurrent();
    }

    public byte[] getResponseBytes() {
        return this.resp;
    }

    public int getSequence() {
        return request.getSequence();
    }

    public int getSession() {
        return request.getSession();
    }

    public void setReply(TOMMessage tomMessage) {
        request.reply = tomMessage;
    }
}
