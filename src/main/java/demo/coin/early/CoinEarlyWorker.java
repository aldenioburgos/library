package demo.coin.early;


import demo.coin.core.requestresponse.CoinMultiOperationContext;
import demo.coin.core.requestresponse.CoinSingleOperationContext;
import demo.coin.core.requestresponse.OperationContext;
import parallelism.hibrid.late.ExtendedLockFreeGraph;
import parallelism.hibrid.late.HibridLockFreeNode;
import parallelism.late.graph.Vertex;

import java.util.Queue;

public class CoinEarlyWorker extends Thread {

    private final int thread_id;
    private final Queue<OperationContext> queue;
    private final ExtendedLockFreeGraph[] subgraphs;

    public CoinEarlyWorker(int id, Queue<OperationContext> queue, ExtendedLockFreeGraph[] subgraphs) {
        super("EarlyWorker-"+id);
        this.thread_id = id;
        this.queue = queue;
        this.subgraphs = subgraphs;
    }

    @Override
    public void run() {
        while (true) {
            OperationContext request = queue.poll();
            if (request == null) {
                continue;
            }
            if (request.isConcurrent()) {
                CoinMultiOperationContext reqs = (CoinMultiOperationContext) request;

                for (int i = 0; i < reqs.getNumOps(); i++) {
                    var singleOperationCtx = new CoinSingleOperationContext(reqs,  i, reqs.getOp(i));
                    var node = new HibridLockFreeNode(singleOperationCtx, Vertex.MESSAGE, subgraphs[thread_id], subgraphs.length, 0);
                    subgraphs[thread_id].insert(node, false, false);
                }
            } else {
                CoinSingleOperationContext req = (CoinSingleOperationContext) request;
                if (req.threadId == thread_id) {
                    req.node.graph = subgraphs[thread_id];
                    subgraphs[thread_id].insert(req.node, false, true);
                } else {
                    subgraphs[thread_id].insert(req.node, true, true);
                }
            }
        }

    }
}
