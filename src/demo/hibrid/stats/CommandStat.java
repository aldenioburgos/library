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
        return "CommandId\t" +
                "earlySchedulerInit\t" +
                "earlySchedulerEnd\t" +
                "earlySchedulerTime\t" +
                "lateSchedulerId\t" +
                "lateSchedulerWaitingInit\t" +
                "lateSchedulerWaitingEnd\t" +
                "lateSchedulerWaitingTime\t" +
                "lateSchedulerSchedulingInit\t" +
                "lateSchedulerSchedulingEnd\t" +
                "lateSchedulerSchedulingTime\t" +
                "replicaWorkerId\t" +
                "replicaWorkerWaitingInit\t" +
                "replicaWorkerWaitingEnd\t" +
                "replicaWorkerWaitingTime\t" +
                "replicaWorkerWorkingInit\t" +
                "replicaWorkerWorkingEnd\t" +
                "replicaWorkerWorkingTime\t" +
                "replicaWorkerRemovalInit\t" +
                "replicaWorkerRemovalEnd\t" +
                "replicaWorkerRemovalTime\t" +
                "replicaWorkerReplyInit\t"+
                "replicaWorkerReplyEnd\t"+
                "replicaWorkerReplyTime\t";
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append(id);
        builder.append('\t');
        builder.append(earlySchedulerInit);
        builder.append('\t');
        builder.append(earlySchedulerEnd);
        builder.append('\t');
        builder.append(earlySchedulerEnd - earlySchedulerInit);
        builder.append('\t');
        builder.append(lateSchedulerId);
        builder.append('\t');
        builder.append(lateSchedulerWaitingInit);
        builder.append('\t');
        builder.append(lateSchedulerWaitingEnd);
        builder.append('\t');
        builder.append(lateSchedulerWaitingEnd - lateSchedulerWaitingInit);
        builder.append('\t');
        builder.append(lateSchedulerInit);
        builder.append('\t');
        builder.append(lateSchedulerEnd);
        builder.append('\t');
        builder.append(lateSchedulerEnd - lateSchedulerInit);
        builder.append('\t');
        builder.append(replicaWorkerId);
        builder.append('\t');
        builder.append(replicaWorkerWaitingInit);
        builder.append('\t');
        builder.append(replicaWorkerWaitingEnd);
        builder.append('\t');
        builder.append(replicaWorkerWaitingEnd - replicaWorkerWaitingInit);
        builder.append('\t');
        builder.append(replicaWorkerInit);
        builder.append('\t');
        builder.append(replicaWorkerEnd);
        builder.append('\t');
        builder.append(replicaWorkerEnd - replicaWorkerInit);
        builder.append('\t');
        builder.append(replicaCommandRemovalInit);
        builder.append('\t');
        builder.append(replicaCommandRemovalEnd);
        builder.append('\t');
        builder.append(replicaCommandRemovalEnd - replicaCommandRemovalInit);
        builder.append('\t');
        builder.append(replicaCommandReplyInit);
        builder.append('\t');
        builder.append(replicaCommandReplyEnd);
        builder.append('\t');
        builder.append(replicaCommandReplyEnd - replicaCommandReplyInit);
        builder.append('\t');
        return builder.toString();
    }
}

