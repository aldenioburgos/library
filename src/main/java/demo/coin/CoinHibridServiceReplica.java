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
import demo.coin.util.ByteArray;
import demo.coin.util.ByteUtils;
import parallelism.ParallelServiceReplica;
import parallelism.hibrid.late.ExtendedLockFreeGraph;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Set;

/**
 * @author aldenio
 */
public class CoinHibridServiceReplica extends ParallelServiceReplica {


    enum PARAMS {
        ID, NUM_PARTITIONS, NUM_LATE_WORKERS, ROOT_PUBLIC_KEY
    }

    private final ExtendedLockFreeGraph[] subgraphs;

    public CoinHibridServiceReplica(int id, Executable executor, Recoverable recoverer, int numPartitions, CoinConflictDefinition cd, int lateWorkers) {
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

    public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException {
        //@formatter:off
        if (args.length != PARAMS.values().length) throw new IllegalArgumentException("Modo de uso:  java  CoinHibridServiceReplica ID NUM_PARTITIONS NUM_LATE_WORKERS ROOT_PUBLIC_KEY");
        //@formatter:on

        int id = Integer.parseInt(args[PARAMS.ID.ordinal()]);
        int lateWorkers = Integer.parseInt(args[PARAMS.NUM_LATE_WORKERS.ordinal()]);
        int numPartitions = Integer.parseInt(args[PARAMS.NUM_PARTITIONS.ordinal()]);
        byte[] rootPubKey = ByteUtils.convertToByteArray(args[PARAMS.ROOT_PUBLIC_KEY.ordinal()]);

        System.out.println("CoinHibridServiceReplica executado com os seguintes argumentos:");
        System.out.println("\tid = " + id);
        System.out.println("\tlateWorkers =" + lateWorkers);
        System.out.println("\tnumPartitions =" + numPartitions);
        System.out.println("\trootPubKey =" + ByteUtils.convertToText(rootPubKey));
        System.out.println("");


        var particoes = new int[numPartitions];
        for (int i = 0; i < numPartitions; i++) {
            particoes[i] = i;
        }
        CoinGlobalState globalState = new CoinGlobalState(Set.of(new ByteArray(rootPubKey)), Collections.emptySet(), particoes);
        Executable executor = new CoinExecutor(globalState);
        CoinConflictDefinition cd = new CoinConflictDefinition();

        new CoinHibridServiceReplica(id, executor, null, numPartitions, cd, lateWorkers);
    }

    @Override
    protected void initWorkers(int n, int id) {
        // não faz nada!
    }
}
