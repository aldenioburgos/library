package demo.hibrid.server;

import java.util.ArrayList;
import java.util.List;

public class StatisticsCollector {

    private static StatisticsCollector instance;

    public long replicaStartingTime;
    public long earlySchedulerSchedule;
    public List<Long> QueuesManagerPutCommandInStart = new ArrayList<>();
    public List<Long> QueuesManagerPutCommandInEnd = new ArrayList<>();

    public RequestStat[] requestStats;
    public LateSchedulerStat[] lateSchedulerStats;

    public static void createInstance(int numPartitions, int numOperations, int numOperationsPerRequest) {
        if (instance == null) {
            instance = new StatisticsCollector(numPartitions, numOperations, numOperationsPerRequest);
        } else {
            throw new IllegalStateException("Tentativa de criar dois coletores de estatistica");
        }
    }

    public static StatisticsCollector getInstance() {
        return instance;
    }

    private StatisticsCollector(int numPartitions, int numOperations, int numOperationsPerRequest) {
        this.lateSchedulerStats = new LateSchedulerStat[numPartitions];
        this.requestStats = new RequestStat[(numOperations / numOperationsPerRequest) + 1];
        this.replicaStartingTime = System.currentTimeMillis();
    }


    public class RequestStat {
        public int id;
        public long arrivalTime;
        public long numCommands;

    }


    public class LateSchedulerStat {
        public int id;
        public List<Long> takeInit = new ArrayList<>();
        public List<Long> takeEndScheduleInit = new ArrayList<>();
        public List<Long> scheduleEnd = new ArrayList<>();
    }
}
