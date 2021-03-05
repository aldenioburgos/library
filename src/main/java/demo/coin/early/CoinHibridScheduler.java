package demo.coin.early;

import bftsmart.tom.core.messages.TOMMessage;
import demo.coin.core.requestresponse.CoinMultiOperationContext;
import demo.coin.core.requestresponse.CoinSingleOperationContext;
import demo.coin.core.requestresponse.OperationContext;
import parallelism.MessageContextPair;
import parallelism.ParallelMapping;
import parallelism.hibrid.early.SPSCQueue;
import parallelism.hibrid.late.HibridLockFreeNode;
import parallelism.late.graph.Vertex;
import parallelism.scheduler.Scheduler;

import java.util.Map;
import java.util.Queue;

public class CoinHibridScheduler implements Scheduler {

    private Map<Integer, CoinHibridClassToThreads> classes;
    public final Queue<OperationContext>[] queues;

    public CoinHibridScheduler(int numberOfPartitions, int queuesCapacity) {
        this.queues = new Queue[numberOfPartitions];
        for (int i = 0; i < queues.length; i++) {
            queues[i] = new SPSCQueue<>(queuesCapacity);
        }
        this.classes = CoinHibridClassToThreads.generateMappings(numberOfPartitions, queues);
    }

    @Override
    public void schedule(TOMMessage request) {
        CoinHibridClassToThreads cToT = classes.get(request.groupId);
        //@formatter:off
        if (cToT == null)   throw new RuntimeException("CLASStoTHREADs MAPPING NOT FOUND");
        //@formatter:on

        CoinMultiOperationContext multiOperationCtx = new CoinMultiOperationContext(request, cToT);
        if (cToT.isConcurrent()) {
            boolean inserted = false;
            while (!inserted) {
                inserted = cToT.queues[0].offer(multiOperationCtx);
            }
        } else {
            int queuesLength = cToT.queues.length;
            for (int i = 0; i < multiOperationCtx.getNumOps(); i++) {
                byte[] operation = multiOperationCtx.getOp(i);
                var singleOperationCtx = new CoinSingleOperationContext(multiOperationCtx, i, operation);
                singleOperationCtx.threadId = cToT.nextThreadIndex();
                singleOperationCtx.node = new HibridLockFreeNode(singleOperationCtx, Vertex.MESSAGE, null, queuesLength, queuesLength);

                for (Queue<OperationContext> queue : cToT.queues) {
                    boolean inserted = false;
                    while (!inserted) {
                        inserted = queue.offer(singleOperationCtx);
                    }
                }
            }

        }
    }

    @Override
    public int getNumWorkers() {
        return queues.length;
    }

    /* **********************
     * Unsupported operations
     * ********************** */

    @Override
    public void schedule(MessageContextPair request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ParallelMapping getMapping() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void scheduleReplicaReconfiguration() {
        throw new UnsupportedOperationException();
    }
}
