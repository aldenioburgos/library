package demo.hibrid.stats;

import demo.hibrid.request.Request;
import demo.hibrid.server.ServerCommand;

import java.util.*;
import java.util.concurrent.Semaphore;

public class Stats {

    // the stats controller
    private static Stats instance;

    // printing thread control related fields
    private Semaphore semaphore = new Semaphore(0);
    private Queue<Integer> readyQueue = new ArrayDeque<>();
    private boolean stop = false;

    // stat related fields
    private Map<Integer, RequestStat> requestStats = new Hashtable<>();


    public static Stats createInstance() {
        if (instance == null) {
            instance = new Stats();
        } else {
            throw new IllegalStateException("Tentativa de criar dois coletores de estatistica");
        }
        return instance;
    }

    public void start() {
        new StatsWorker().start();
    }

    public static void messageReceive(Request request) {
        var reqStat = new RequestStat(request.getId(), request.getCommands().length);
        instance.requestStats.put(request.getId(), reqStat);
    }

    public static void earlySchedulerInit(ServerCommand command) {
        instance.requestStats.get(command.requestId).getCommand(command.commandId).id = command.commandId;
        instance.requestStats.get(command.requestId).getCommand(command.commandId).earlySchedulerInit = System.nanoTime();
    }

    public static void earlySchedulerEnd(ServerCommand command) {
        instance.requestStats.get(command.requestId).getCommand(command.commandId).earlySchedulerEnd = System.nanoTime();
    }

    public static void lateSchedulerInit(int lateSchedulerId, long takeInit, ServerCommand command) {
        var lateSchedulerInit = System.nanoTime();
        instance.requestStats.get(command.requestId).getCommand(command.commandId).lateSchedulerId = lateSchedulerId;
        instance.requestStats.get(command.requestId).getCommand(command.commandId).lateSchedulerWaitingInit = takeInit;
        instance.requestStats.get(command.requestId).getCommand(command.commandId).lateSchedulerWaitingEnd = lateSchedulerInit;
        instance.requestStats.get(command.requestId).getCommand(command.commandId).lateSchedulerInit = lateSchedulerInit;
    }

    public static void lateSchedulerEnd(int id, ServerCommand command) {
        instance.requestStats.get(command.requestId).getCommand(command.commandId).lateSchedulerEnd = System.nanoTime();
    }

    public static void replicaWorkerInit(int id, long workerInit, ServerCommand command) {
        var replicaWorkerInit = System.nanoTime();
        instance.requestStats.get(command.requestId).getCommand(command.commandId).replicaWorkerId = id;
        instance.requestStats.get(command.requestId).getCommand(command.commandId).replicaWorkerWaitingInit = workerInit;
        instance.requestStats.get(command.requestId).getCommand(command.commandId).replicaWorkerWaitingEnd = replicaWorkerInit;
        instance.requestStats.get(command.requestId).getCommand(command.commandId).replicaWorkerInit = replicaWorkerInit;
    }

    public static void replicaWorkerEnd(int id, ServerCommand command) {
        var replicaWorkerEnd = System.nanoTime();
        instance.requestStats.get(command.requestId).getCommand(command.commandId).replicaWorkerEnd = replicaWorkerEnd;
        instance.requestStats.get(command.requestId).getCommand(command.commandId).replicaCommandRemovalInit = replicaWorkerEnd;
    }

    public static void commandRemoved(int id, ServerCommand command) {
        var replicaCommandRemovalEnd = System.nanoTime();
        instance.requestStats.get(command.requestId).getCommand(command.commandId).replicaCommandRemovalEnd = replicaCommandRemovalEnd;
        instance.requestStats.get(command.requestId).getCommand(command.commandId).replicaCommandReplyInit = replicaCommandRemovalEnd;
    }

    public static void replySent(int id, ServerCommand command) {
        if (instance.requestStats.get(command.requestId) != null) {
            instance.requestStats.get(command.requestId).getCommand(command.commandId).replicaCommandReplyEnd = System.nanoTime();
        }
    }

    public static void messageReply(int requestId) {
        instance.requestStats.get(requestId).replyTime = System.nanoTime();
        instance.readyQueue.add(requestId);
        instance.semaphore.release();
    }

    public static void print(){
        System.out.println(RequestStat.title());
        var builder = new StringBuilder();
        for (var x :instance.requestStats.values()) {
            builder.append(x.toString());
        }
        System.out.println(builder.toString());
    }

    public static void messageScheduled(Request request) {
        instance.requestStats.get(request.getId()).scheduledAt = System.nanoTime();
    }

    /**
     * The Stat printer thread.
     */
    public class StatsWorker extends Thread {
        @Override
        public void run() {
            try {
                System.out.println(RequestStat.title());
                while (!instance.stop) {
                    semaphore.acquire();
                    var readyMsgId = readyQueue.poll();
                    print(readyMsgId);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        private void print(Integer readyMsgId) {
            var request = requestStats.remove(readyMsgId);
            System.out.println(request.toString());
        }
    }

}
