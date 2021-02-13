package demo.coin;

import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.util.MultiOperationRequest;
import parallelism.hibrid.early.HibridClassToThreads;
import parallelism.hibrid.early.HibridScheduler;

public class CoinHibridScheduler extends HibridScheduler {

    public CoinHibridScheduler(int numberOfPartitions, HibridClassToThreads[] cToT, int queuesCapacity) {
        super(numberOfPartitions, cToT, queuesCapacity);
    }

    @Override
    protected MultiOperationRequest getNewMultiOperationRequest(TOMMessage request) {
        return new CoinMultiOperationRequest(request.getContent());
    }
}
