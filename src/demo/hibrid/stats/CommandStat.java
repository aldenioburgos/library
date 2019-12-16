package demo.hibrid.stats;

public class CommandStat {
    public int id;
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
    public long replicaCommandReplyEnd;
    public long replicaCommandReplyInit;
    public long replicaCommandRemovalEnd;
    public long replicaCommandRemovalInit;

    public static String title() {
        return "CommandId\tearlySchedulerTime\tlateSchedulerId\tlateSchedulerWaitingTime\tlateSchedulerSchedulingTime\treplicaWorkerId\treplicaWorkerWaitingTime\treplicaWorkerWorkingTime\treplicaWorkerRemovalTime\treplicaWorkerReplyTime";
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append(id);
        builder.append('\t');
        builder.append(earlySchedulerEnd - earlySchedulerInit);
        builder.append('\t');
        builder.append(lateSchedulerId);
        builder.append('\t');
        builder.append(lateSchedulerWaitingEnd - lateSchedulerWaitingInit);
        builder.append('\t');
        builder.append(lateSchedulerEnd - lateSchedulerInit);
        builder.append('\t');
        builder.append(replicaWorkerId);
        builder.append('\t');
        builder.append(replicaWorkerWaitingEnd - replicaWorkerWaitingInit);
        builder.append('\t');
        builder.append(replicaWorkerEnd - replicaWorkerInit);
        builder.append('\t');
        builder.append(replicaCommandRemovalEnd - replicaCommandRemovalInit);
        builder.append('\t');
        builder.append(replicaCommandReplyEnd - replicaCommandReplyInit);
        builder.append('\t');
        return builder.toString();
    }
}

