package demo.coin.late;

import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.Replier;
import bftsmart.tom.server.SingleExecutable;
import bftsmart.util.ThroughputStatistics;
import demo.coin.core.requestresponse.CoinOperationContext;
import parallelism.hibrid.late.ExtendedLockFreeGraph;
import parallelism.hibrid.late.HibridLockFreeNode;

public class CoinLateWorker extends Thread {

    private final int replicaId;
    private final int threadId;
    private final int myPartition;
    private ExtendedLockFreeGraph[] subgraphs;
    private SingleExecutable executor;
    private Replier replier;
    private ServerViewController SVController;
    private ThroughputStatistics statistics;

    public CoinLateWorker(int replicaId,
                          int threadId,
                          int partitions,
                          ExtendedLockFreeGraph[] subgraphs,
                          SingleExecutable executor,
                          Replier replier,
                          ServerViewController SVController,
                          ThroughputStatistics statistics) {
        super("LateWorker-" + threadId);
        this.replicaId = replicaId;
        this.threadId = threadId;
        this.myPartition = threadId % partitions;
        this.subgraphs = subgraphs;
        this.executor = executor;
        this.replier = replier;
        this.SVController = SVController;
        this.statistics = statistics;
    }

    public void run() {
        while (true) {
            try {
                HibridLockFreeNode node = subgraphs[this.myPartition].get();
                CoinOperationContext msg = (CoinOperationContext) node.getData();
                msg.resp = executor.executeOrdered(msg.operation, null);
                msg.setReply(new TOMMessage(replicaId, msg.getSession(), msg.getSequence(), msg.getResponseBytes(), SVController.getCurrentViewId()));
                replier.manageReply(msg.getTOMRequest(), null);
                statistics.computeStatistics(threadId, 1, msg.request.getId());
                //remove
                subgraphs[this.myPartition].remove(node);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
