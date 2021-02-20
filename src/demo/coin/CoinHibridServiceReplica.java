package demo.coin;

import bftsmart.tom.server.Executable;
import bftsmart.tom.server.Recoverable;
import bftsmart.tom.server.SingleExecutable;
import bftsmart.util.ThroughputStatistics;
import parallelism.ParallelServiceReplica;
import parallelism.hibrid.early.EarlySchedulerMapping;
import parallelism.hibrid.late.ExtendedLockFreeGraph;
import parallelism.late.ConflictDefinition;

/**
 * @author aldenio
 */
public class CoinHibridServiceReplica extends ParallelServiceReplica {

    private final ExtendedLockFreeGraph[] subgraphs;

    public CoinHibridServiceReplica(int id, Executable executor, Recoverable recoverer, int numPartitions, ConflictDefinition cd, int lateWorkers) {
        super(id, executor, recoverer, numPartitions);
        System.out.println("Criou um hibrid scheduler: partitions (early) = " + numPartitions + " workers (late) = " + lateWorkers);

        statistics = new ThroughputStatistics(id, lateWorkers, "resultsHibrid_" + id + "_" + numPartitions + "_" + lateWorkers + ".txt", "");
        subgraphs = createSubGraphs(numPartitions, cd);
        initLateWorkers(lateWorkers, id, numPartitions);
    }

    private ExtendedLockFreeGraph[] createSubGraphs(int numPartitions, ConflictDefinition cd) {
        var graphs = new ExtendedLockFreeGraph[numPartitions];
        for (int i = 0; i < numPartitions; i++) {
            graphs[i] = new ExtendedLockFreeGraph(cd, i, 150 / numPartitions);
        }
        return graphs;
    }


    @Override
    protected void createScheduler(int initialWorkers) {
        if (initialWorkers <= 0) {
            initialWorkers = 1;
        }
        this.scheduler = new CoinHibridScheduler(initialWorkers, new EarlySchedulerMapping().generateMappings(initialWorkers), 100000000);
    }

    @Override
    protected void initWorkers(int n, int id) {
        System.out.println("n early: " + n);
        for (int i = 0; i < n; i++) {
            new CoinEarlyWorker(i, ((CoinHibridScheduler) this.scheduler).getAllQueues()[i], (CoinHibridScheduler) this.scheduler, subgraphs).start();
        }
    }

    protected void initLateWorkers(int n, int id, int partitions) {
        System.out.println("n late: " + n);
        for (int i = 0; i < n; i++) {
            new CoinLateWorker(id, partitions, subgraphs, (SingleExecutable) executor, replier, SVController, statistics).start();
        }
    }


}
