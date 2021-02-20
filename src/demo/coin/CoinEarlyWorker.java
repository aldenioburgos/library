package demo.coin;


import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.util.MultiOperationRequest;
import parallelism.MessageContextPair;
import parallelism.MultiOperationCtx;
import parallelism.hibrid.early.HibridClassToThreads;
import parallelism.hibrid.early.HibridScheduler;
import parallelism.hibrid.early.TOMMessageWrapper;
import parallelism.hibrid.late.ExtendedLockFreeGraph;
import parallelism.hibrid.late.HibridLockFreeNode;
import parallelism.late.graph.Vertex;

import java.util.Queue;

public class CoinEarlyWorker extends Thread {

    private final int thread_id;
    private Queue<TOMMessage> reqs;
    private CoinHibridScheduler scheduler;
    private ExtendedLockFreeGraph[] subgraphs;

    public CoinEarlyWorker(int id, Queue<TOMMessage> reqs, CoinHibridScheduler scheduler, ExtendedLockFreeGraph[] subgraphs) {
        this.thread_id = id;
        this.reqs = reqs;
        this.scheduler = scheduler;
        this.subgraphs = subgraphs;
    }

    @Override
    public void run() {
        while (true) {
            TOMMessage request = reqs.poll();
            if (request != null) {
                HibridClassToThreads ct = ((HibridScheduler) scheduler).getClass(request.getGroupId());
                if (ct.type == HibridClassToThreads.CONC) {
                    MultiOperationRequest reqs = new MultiOperationRequest(request.getContent());
                    MultiOperationCtx ctx = new MultiOperationCtx(reqs.operations.length, request);
                    for (int i = 0; i < reqs.operations.length; i++) {
                        subgraphs[thread_id].insert(new HibridLockFreeNode(new MessageContextPair(request, request.groupId, i, reqs.operations[i], reqs.opId, ctx), Vertex.MESSAGE, subgraphs[thread_id], subgraphs.length, 0), false, false);
                    }
                } else if (ct.type == HibridClassToThreads.SYNC) {
                    TOMMessageWrapper mw = (TOMMessageWrapper) request;

                    if (mw.msg.threadId == thread_id) {
                        mw.msg.node.graph = subgraphs[thread_id];
                        subgraphs[thread_id].insert(mw.msg.node, false, true);
                    } else {
                        subgraphs[thread_id].insert(mw.msg.node, true, true);
                    }
                }
            }
        }
    }
}
