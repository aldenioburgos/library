package demo.coin;

import bftsmart.tom.server.Executable;
import bftsmart.tom.server.Recoverable;
import bftsmart.tom.server.SingleExecutable;
import bftsmart.util.ThroughputStatistics;
import demo.coin.core.CoinGlobalState;
import demo.coin.late.CoinExecutor;
import parallelism.ParallelServiceReplica;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * @author aldenio
 */
public class CoinSequentialServiceReplica extends ParallelServiceReplica {


    enum PARAMS {
        ID,  WARM_UP_FILE
    }

    public CoinSequentialServiceReplica(int id, Executable executor, Recoverable recoverer, int numPartitions) {
        super(id, executor, recoverer, numPartitions);
        System.out.println("Criou um sequential scheduler!");
    }

    @Override
    protected void createScheduler(int numPartitions) {
        this.statistics = new ThroughputStatistics(id, 1, "resultsCoinReplica_" + id + "-sequencial.txt", "");
        this.scheduler = new CoinSequentialScheduler(id, (SingleExecutable) executor, replier, statistics, SVController);
    }

    public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        //@formatter:off
        if (args.length != PARAMS.values().length) throw new IllegalArgumentException("Modo de uso:  java  CoinSequentialServiceReplica ID WARM_UP_FILE");
        //@formatter:on

        int id = Integer.parseInt(args[PARAMS.ID.ordinal()]);
        String warmUpFile = args[PARAMS.WARM_UP_FILE.ordinal()];

        System.out.println("CoinSequentialServiceReplica executado com os seguintes argumentos:");
        System.out.println("\tid = " + id);
        System.out.println("\twarm-up file = " + warmUpFile);
        WarmUp warmUp = WarmUp.loadFrom(warmUpFile);

        CoinGlobalState globalState = new CoinGlobalState(warmUp);
        new CoinSequentialServiceReplica(id, new CoinExecutor(globalState), null, warmUp.numPartitions);
    }

    @Override
    protected void initWorkers(int n, int id) {
        // não faz nada!
    }
}
