package demo.coin.early;


import demo.coin.core.requestresponse.CoinOperationContext;
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
            CoinOperationContext operationContext = (CoinOperationContext) request;
            if (request.isConcurrent()) {
                operationContext.node = new HibridLockFreeNode(operationContext, Vertex.MESSAGE, subgraphs[thread_id], subgraphs.length, 0);
                subgraphs[thread_id].insert(operationContext.node, false, false);
            } else {
                if (operationContext.threadId == thread_id) {
                    operationContext.node.graph = subgraphs[thread_id];
                    subgraphs[thread_id].insert(operationContext.node, false, true);
                } else {
                    subgraphs[thread_id].insert(operationContext.node, true, true);
                }
            }
        }

    }
}
