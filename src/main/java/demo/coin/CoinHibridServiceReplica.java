package demo.coin;

import bftsmart.tom.server.Executable;
import bftsmart.tom.server.Recoverable;
import bftsmart.tom.server.SingleExecutable;
import bftsmart.util.ThroughputStatistics;
import demo.coin.early.CoinEarlyWorker;
import demo.coin.early.CoinHibridScheduler;
import demo.coin.late.CoinConflictDefinition;
import demo.coin.late.CoinLateWorker;
import parallelism.ParallelServiceReplica;
import parallelism.hibrid.late.ExtendedLockFreeGraph;

/**
 * @author aldenio
 */
public class CoinHibridServiceReplica extends ParallelServiceReplica {

    private final ExtendedLockFreeGraph[] subgraphs;

    public CoinHibridServiceReplica(int id, Executable executor, Recoverable recoverer, int numPartitions, CoinConflictDefinition cd, int lateWorkers) {
        super(id, executor, recoverer, numPartitions);
        System.out.println("Criou um hibrid scheduler: partitions (early) = " + numPartitions + " workers (late) = " + lateWorkers);

        statistics = new ThroughputStatistics(id, lateWorkers, "resultsHibrid_" + id + "_" + numPartitions + "_" + lateWorkers + ".txt", "");
        subgraphs = createSubGraphs(numPartitions, cd);
        initLateWorkers(lateWorkers, id, numPartitions);
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

    @Override
    protected void initWorkers(int n, int id) {
        System.out.println("n early: " + n);
        for (int i = 0; i < n; i++) {
            new CoinEarlyWorker(i, ((CoinHibridScheduler) this.scheduler).queues[i], subgraphs).start();
        }
    }

    protected void initLateWorkers(int n, int id, int partitions) {
        System.out.println("n late: " + n);
        for (int i = 0; i < n; i++) {
            new CoinLateWorker(id, partitions, subgraphs, (SingleExecutable) executor, replier, SVController, statistics).start();
        }
    }

}
