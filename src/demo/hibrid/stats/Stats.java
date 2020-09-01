package demo.hibrid.stats;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.OFF;

public class Stats {

    // the stats controller
    private static final Stats instance = new Stats();
    private static final java.lang.System.Logger logger = System.getLogger(Stats.class.getName());

    // printing thread control related fields
    private final Semaphore semaphore = new Semaphore(0);
    private final Queue<Integer> readyQueue = new ConcurrentLinkedQueue<>();

    // stat related fields
    private final Map<Integer, Queue<Event>> logs = new ConcurrentHashMap<>();

    private Stats() {
        new StatsWorker().start();
    }

    public static Stats getInstance() {
        return instance;
    }

    public static void log(Event event) {
        System.out.println(event);
//        Integer requestId = (event.getRequestId() == null) ? Integer.valueOf(-1) : event.getRequestId();
//        if (getInstance().logs.get(requestId) == null) {
//            getInstance().logs.put(requestId, new ConcurrentLinkedQueue<>());
//        }
//        getInstance().logs.get(requestId).add(event);
//        if (event.getType() == EventType.REPLY_SENT) {
//            getInstance().readyQueue.add(requestId);
//            getInstance().semaphore.release();
//        }
    }

    private String title() {
        return "*** Tudo está em nanosecs *** \t" +
                "RequestId\t" +
                "arrivalTime\t" +
                "scheduledAt\t" +
                "replyTime\t" +
                "CommandId\t" +
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
                "replicaWorkerReplyInit\t" +
                "replicaWorkerReplyEnd\t" +
                "replicaWorkerReplyTime\t";
                //TODO calcular o número de operações por segundo
    }

    public void print(Collection<Event> events) {
        var builder = new StringBuilder();
        for (var event : events) {
            builder.append(event.toString());
        }
        logger.log(OFF, builder.toString());
    }


    /**
     * The Stat printer thread.
     */
    public class StatsWorker extends Thread {

        @Override
        public void run() {
            try {
                logger.log(INFO, title());
                while (true) {
                    semaphore.acquire();
                    var readyMsgId = readyQueue.poll();
                    var events = logs.remove(readyMsgId);
                    print(events);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
