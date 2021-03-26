package demo.coin.core.requestresponse;

import bftsmart.tom.core.messages.TOMMessage;
import demo.coin.early.CoinHibridClassToThreads;

public class CoinMultiOperationContext implements OperationContext {

    public CoinMultiOperationRequest multiOperationRequest;
    public CoinMultiOperationResponse multiOperationResponse;
    public CoinHibridClassToThreads cToT;

    public CoinMultiOperationContext(TOMMessage request, CoinHibridClassToThreads cToT) {
        this.multiOperationRequest = new CoinMultiOperationRequest(request);
        this.multiOperationResponse = new CoinMultiOperationResponse(multiOperationRequest.getNumOps());
        this.cToT = cToT;
    }


    public void add(int index, byte[] resp) {
        this.multiOperationResponse.add(index, resp);
    }

    public boolean isComplete() {
        return multiOperationResponse.isComplete();
    }

    public TOMMessage getTOMRequest() {
        return multiOperationRequest.request;
    }

    public int getNumOps() {
        return multiOperationRequest.getNumOps();
    }

    public byte[] getOp(int i) {
        return multiOperationRequest.getOp(i);
    }

    public CoinHibridClassToThreads getClassToThreadMap() {
        return cToT;
    }

    @Override
    public boolean isConcurrent() {
        return cToT.isConcurrent();
    }

    @Override
    public int getClassId() {
        return multiOperationRequest.request.groupId;
    }
}
