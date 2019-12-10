package demo.hibrid.stats;

public class CommandStat {
    //early scheduler
    public long earlySchedulerInit;
    public long earlySchedulerEnd;

    // late scheduler
    public int lateSchedulerId;
    public long lateSchedulerWaitingInit;
    public long lateSchedulerWaitingEnd;
    public long lateSchedulerInit;
    public long lateSchedulerEnd;

    // replica worker
    public int replicaWorkerId;
    public long replicaWorkerInit;
    public long replicaWorkerWaitingInit;
    public long replicaWorkerWaitingEnd;
    public long replicaWorkerEnd;
}
