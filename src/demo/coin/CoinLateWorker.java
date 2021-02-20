package demo.coin;

import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.Replier;
import bftsmart.tom.server.SingleExecutable;
import bftsmart.util.ThroughputStatistics;
import parallelism.hibrid.late.ExtendedLockFreeGraph;
import parallelism.hibrid.late.HibridLockFreeNode;

public class CoinLateWorker extends Thread {

    private final int thread_id;
    private final int myPartition;
    private ExtendedLockFreeGraph[] subgraphs;
    private SingleExecutable executor;
    private Replier replier;
    private ServerViewController SVController;
    private ThroughputStatistics statistics;

    public CoinLateWorker(int id, int partitions, ExtendedLockFreeGraph[] subgraphs, SingleExecutable executor, Replier replier, ServerViewController SVController, ThroughputStatistics statistics) {
        this.thread_id = id;
        this.myPartition = id % partitions;
        this.subgraphs = subgraphs;
        this.executor = executor;
        this.replier = replier;
        this.SVController = SVController;
        this.statistics = statistics;
    }

    public void run() {
        while (true) {
            try {
                //get
                HibridLockFreeNode node = subgraphs[this.myPartition].get();
                //execute
                CoinMessageContextPair msg  = (CoinMessageContextPair)node.getAsRequest();
                msg.resp = executor.executeOrdered( null/*TODO bytes da operação*/, null);

                msg.ctx.add(msg.index, msg.resp);
                if (msg.ctx.response.isComplete() && !msg.ctx.finished && (msg.ctx.interger.getAndIncrement() == 0)) {
                    msg.ctx.finished = true;
                    msg.ctx.request.reply = new TOMMessage(thread_id, msg.ctx.request.getSession(), msg.ctx.request.getSequence(), msg.ctx.response.serialize(), SVController.getCurrentViewId());
                    replier.manageReply(msg.ctx.request, null);
                }
                statistics.computeStatistics(thread_id, 1);
                //remove
                subgraphs[this.myPartition].remove(node);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}
