package demo.hibrid.stats;

import bftsmart.tom.core.messages.TOMMessage;
import demo.hibrid.request.Request;
import demo.hibrid.server.ServerCommand;

import javax.print.DocFlavor;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class Stats {

    // the stats controller
    private static Stats instance;

    // printing thread control related fields
    private Semaphore semaphore = new Semaphore(0);
    private Queue<Integer> readyQueue = new ArrayDeque<>();
    public boolean stop = false;

    // stat related fields
    private int numPartitions;
    private int numOperations;
    private int numOperationsPerRequest;
    private long replicaStartingTime;
    private Map<Integer, RequestStat> requestStats = new HashMap<>();
    public LateSchedulerStat[] lateSchedulerStats;


    public static Stats createInstance(int numPartitions, int numOperations, int numOperationsPerRequest) {
        if (instance == null) {
            instance = new Stats(numPartitions, numOperations, numOperationsPerRequest);
        } else {
            throw new IllegalStateException("Tentativa de criar dois coletores de estatistica");
        }
        return instance;
    }

    public Stats(int numPartitions, int numOperations, int numOperationsPerRequest) {
        this.numPartitions = numPartitions;
        this.numOperations = numOperations;
        this.numOperationsPerRequest = numOperationsPerRequest;
        this.replicaStartingTime = System.currentTimeMillis();
        this.lateSchedulerStats = createLateSchedulerStats(numPartitions, numOperations);
        new StatsWorker().start();
    }

    private LateSchedulerStat[] createLateSchedulerStats(int numPartitions, int numOperations) {
        var lateSchedulerStats = new LateSchedulerStat[numPartitions];
        for (int i = 0; i < numPartitions; i++) {
            lateSchedulerStats[i] = new LateSchedulerStat(i, numOperations);
        }
        return lateSchedulerStats;
    }


    public static void messageReceive(Request request) {
        var reqStat = new RequestStat(request.getId(), request.getCommands().length);
        reqStat.arrivalTime = System.currentTimeMillis();
        instance.requestStats.put(reqStat.id, reqStat);
        System.out.println("Received Request " + reqStat.id + " with " +  request.getCommands().length + " commands.");
    }

    public static void messageReply(int requestId) {
        instance.requestStats.get(requestId).replyTime = System.currentTimeMillis();
        instance.readyQueue.add(requestId);
        instance.semaphore.release(1);
    }

    public static void earlySchedulerInit(ServerCommand command) {
        var earlySchedulerInit = System.currentTimeMillis();
        instance.requestStats.get(command.requestId).getCommand(command.getCommandId()).earlySchedulerInit = earlySchedulerInit;
        System.out.println("EarlySchedulerInit for "+command);
    }

    public static void earlySchedulerEnd(ServerCommand command) {
        var earlySchedulerEnd = System.currentTimeMillis();
        instance.requestStats.get(command.requestId).getCommand(command.getCommandId()).earlySchedulerEnd = earlySchedulerEnd;
        System.out.println("EarlySchedulerEnd for "+command);
    }

    public static void lateSchedulerInit(int lateSchedulerId, long takeInit, ServerCommand command) {
        var lateSchedulerInit = System.currentTimeMillis();
        instance.requestStats.get(command.requestId).getCommand(command.getCommandId()).lateSchedulerId = lateSchedulerId;
        instance.requestStats.get(command.requestId).getCommand(command.getCommandId()).lateSchedulerWaitingInit = takeInit;
        instance.requestStats.get(command.requestId).getCommand(command.getCommandId()).lateSchedulerWaitingEnd = lateSchedulerInit;
        instance.requestStats.get(command.requestId).getCommand(command.getCommandId()).lateSchedulerInit = lateSchedulerInit;
        System.out.println("lateScheduler "+lateSchedulerId+" Init for "+command);
    }

    public static void lateSchedulerEnd(int id, ServerCommand command) {
        var lateSchedulerEnd = System.currentTimeMillis();
        instance.requestStats.get(command.requestId).getCommand(command.getCommandId()).lateSchedulerEnd = lateSchedulerEnd;
        System.out.println("lateScheduler "+id+" End for "+command);
    }

    public static void replicaWorkerInit(int id, long workerInit, ServerCommand command) {
        var replicaWorkerInit = System.currentTimeMillis();
        instance.requestStats.get(command.requestId).getCommand(command.getCommandId()).replicaWorkerId = id;
        instance.requestStats.get(command.requestId).getCommand(command.getCommandId()).replicaWorkerWaitingInit = workerInit;
        instance.requestStats.get(command.requestId).getCommand(command.getCommandId()).replicaWorkerWaitingEnd = replicaWorkerInit;
        instance.requestStats.get(command.requestId).getCommand(command.getCommandId()).replicaWorkerInit = replicaWorkerInit;
        System.out.println("replicaWorker "+id+" Init for "+command);
    }

    public static void replicaWorkerEnd(int id, ServerCommand command) {
        var replicaWorkerEnd = System.currentTimeMillis();
        instance.requestStats.get(command.requestId).getCommand(command.getCommandId()).replicaWorkerEnd = replicaWorkerEnd;
        System.out.println("replicaWorker "+id+" End for "+command);
    }


    /**
     * The Stat printer thread.
     */
    public class StatsWorker extends Thread {
        @Override
        public void run() {
            try {
                while (!instance.stop) {
                    semaphore.acquire();
                    var readyMsgId = readyQueue.poll();
                    print(readyMsgId);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void print(Integer readyMsgId) {
            var request = requestStats.remove(readyMsgId);

        }
    }

}
