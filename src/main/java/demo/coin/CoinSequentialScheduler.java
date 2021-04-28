package demo.coin;

import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.Replier;
import bftsmart.tom.server.SingleExecutable;
import bftsmart.util.ThroughputStatistics;
import demo.coin.core.requestresponse.CoinOperationContext;
import parallelism.MessageContextPair;
import parallelism.ParallelMapping;
import parallelism.scheduler.Scheduler;

public class CoinSequentialScheduler implements Scheduler {
    private Integer replicaId;
    private SingleExecutable executor;
    private Replier replier;
    private ThroughputStatistics statistics;
    private ServerViewController SVController;

    public CoinSequentialScheduler(Integer replicaId, SingleExecutable executor, Replier replier, ThroughputStatistics statistics, ServerViewController SVController) {
        this.replicaId = replicaId;
        this.executor = executor;
        this.replier = replier;
        this.statistics = statistics;
        this.SVController = SVController;
    }

    @Override
    public void schedule(TOMMessage request) {
        // recebe um batch de operações no request
        CoinOperationContext operationContext = new CoinOperationContext(request, null);
        // executa
        operationContext.resp = executor.executeOrdered(operationContext.operation, null);
        // se terminou o batch responde
        operationContext.setReply(new TOMMessage(replicaId, operationContext.getSession(), operationContext.getSequence(), operationContext.getResponseBytes(), SVController.getCurrentViewId()));
        replier.manageReply(operationContext.getTOMRequest(), null);
        // computa a estatística
        statistics.computeStatistics(0, 1);
    }

    @Override
    public int getNumWorkers() {
        return 1;
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
