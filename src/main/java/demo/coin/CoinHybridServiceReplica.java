package demo.coin;

import bftsmart.tom.server.Executable;
import bftsmart.tom.server.Recoverable;
import bftsmart.tom.server.SingleExecutable;
import bftsmart.util.ThroughputStatistics;
import demo.coin.core.CoinGlobalState;
import demo.coin.early.CoinEarlyWorker;
import demo.coin.early.CoinHibridScheduler;
import demo.coin.late.CoinConflictDefinition;
import demo.coin.late.CoinExecutor;
import demo.coin.late.CoinLateWorker;
import parallelism.ParallelServiceReplica;
import parallelism.hibrid.late.ExtendedLockFreeGraph;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * @author aldenio
 */
public class CoinHybridServiceReplica extends ParallelServiceReplica {


    enum PARAMS {
        ID, LATE_WORKERS_PER_PARTITION, WARM_UP_FILE
    }

    private final ExtendedLockFreeGraph[] subgraphs;

    public CoinHybridServiceReplica(int id, Executable executor, Recoverable recoverer, int numPartitions, CoinConflictDefinition cd, int lateWorkers) {
        super(id, executor, recoverer, numPartitions);
        System.out.println("Criou um hibrid scheduler: partitions (early) = " + numPartitions + " workers (late) = " + lateWorkers);

        statistics = new ThroughputStatistics(id, lateWorkers, "resultsHibrid_" + id + "_" + numPartitions + "_" + lateWorkers + ".txt", "");
        subgraphs = createSubGraphs(numPartitions, cd);
        initEarlyWorkers(numPartitions);
        initLateWorkers(lateWorkers, numPartitions, statistics);
    }

    private ExtendedLockFreeGraph[] createSubGraphs(int numPartitions, CoinConflictDefinition cd) {
        var graphs = new ExtendedLockFreeGraph[numPartitions];
        for (int i = 0; i < numPartitions; i++) {
            graphs[i] = new ExtendedLockFreeGraph(cd, i, 150 / numPartitions);
        }
        return graphs;
    }

    @Override
    protected void createScheduler(int numPartitions) {
        if (numPartitions <= 0) {
            numPartitions = 1;
        }
        this.scheduler = new CoinHibridScheduler(numPartitions, 100000000);
    }


    protected void initEarlyWorkers(int n) {
        System.out.println("n early: " + n);
        for (int i = 0; i < n; i++) {
            new CoinEarlyWorker(i, ((CoinHibridScheduler) this.scheduler).queues[i], subgraphs).start();
        }
    }

    protected void initLateWorkers(int n, int partitions, ThroughputStatistics statistics) {
        System.out.println("n late: " + n);
        for (int i = 0; i < n; i++) {
            new CoinLateWorker(i, partitions, subgraphs, (SingleExecutable) executor, replier, SVController, statistics).start();
        }
    }

    public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        //@formatter:off
        if (args.length != PARAMS.values().length) throw new IllegalArgumentException("Modo de uso:  java  CoinHibridServiceReplica ID LATE_WORKERS_PER_PARTITION WARM_UP_FILE");
        //@formatter:on

        int id = Integer.parseInt(args[PARAMS.ID.ordinal()]);
        int lateWorkersPerPartition = Integer.parseInt(args[PARAMS.LATE_WORKERS_PER_PARTITION.ordinal()]);
        String warmUpFile = args[PARAMS.WARM_UP_FILE.ordinal()];

        System.out.println("CoinHibridServiceReplica executado com os seguintes argumentos:");
        System.out.println("\tid = " + id);
        System.out.println("\tlateWorkersPerPartition = " + lateWorkersPerPartition);
        System.out.println("\twarm-up file = " + warmUpFile);
        WarmUp warmUp = WarmUp.loadFrom(warmUpFile);

        CoinGlobalState globalState = new CoinGlobalState(warmUp);
        new CoinHybridServiceReplica(id, new CoinExecutor(globalState), null, warmUp.numPartitions, new CoinConflictDefinition(), warmUp.numPartitions*lateWorkersPerPartition);
    }

    @Override
    protected void initWorkers(int n, int id) {
        // não faz nada!
    }
}
