package demo.coin;

import bftsmart.tom.ParallelServiceProxy;
import demo.coin.core.Utxo;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

enum PARAMS {
    ID, CHAVE, NUM_THREADS_CLIENTE, PERC_GLOBAL, PERC_WRITE, WARM_UP_FILE
}


public class CoinClient {

    public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException {
        //@formatter:off
        if (args.length != PARAMS.values().length) throw new IllegalArgumentException("Modo de uso:  java CoinClient ID, CHAVE, NUM_THREADS_CLIENTE, PERC_GLOBAL, PERC_WRITE, WARM_UP_FILE");
        //@formatter:on

        int numClientes = Integer.parseInt(args[PARAMS.NUM_THREADS_CLIENTE.ordinal()]);
        int id = Integer.parseInt(args[PARAMS.ID.ordinal()]);
        int percGlobal = Integer.parseInt(args[PARAMS.PERC_GLOBAL.ordinal()]);
        int percWrite = Integer.parseInt(args[PARAMS.PERC_WRITE.ordinal()]);
        int chave = Integer.parseInt(args[PARAMS.CHAVE.ordinal()]);
        String warmUpFile = args[PARAMS.WARM_UP_FILE.ordinal()];


        System.out.println("CoinClient executado com os seguintes argumentos:");
        System.out.println("\tid = " + id);
        System.out.println("\tchave = " + chave);
        System.out.println("\tnumClientes = " + numClientes);
        System.out.println("\tpercGlobal = " + percGlobal);
        System.out.println("\tpercWrite = " + percWrite);
        System.out.println("\twarm-up file = " + warmUpFile);
        WarmUp warmUp = WarmUp.loadFrom(warmUpFile);

        CoinClient.run(id, chave, numClientes, percGlobal, percWrite, warmUp);
    }

    public static void run(int id, int chave,  int numClientes, int percGlobal, int percWrite, WarmUp warmUp) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        //@formatter:off
        if (id < 0)                                         throw new IllegalArgumentException("id="+id);
        if (numClientes <= 0)                               throw new IllegalArgumentException("numClientes="+numClientes);
        if (percGlobal < 0 || percGlobal > 100)             throw new IllegalArgumentException("percGlobal="+percGlobal);
        if (percWrite < 0 || percWrite > 100)               throw new IllegalArgumentException("percWrite="+percWrite);
        if (warmUp == null)                                 throw new IllegalArgumentException("warmUp=null");
        if (warmUp.tokens.isEmpty())                        throw new IllegalArgumentException("warmUp.tokens.size=0");
        if ((id+1) * numClientes > warmUp.users.size())     throw new IllegalArgumentException("insuficient users in warmup file");
        if (warmUp.numPartitions < 1)                       throw new IllegalArgumentException("Can't instantiate experiment with less than one partition");
        if (warmUp.numPartitions== 1 && percGlobal > 0)     throw new IllegalArgumentException("Single partition experiments does not accept global commands");
        //@formatter:on

        KeyPair[] users = warmUp.users.toArray(KeyPair[]::new);
        Utxo[] tokens = warmUp.tokens.toArray(Utxo[]::new);
        int numPartitions = warmUp.numPartitions;
        List<CoinClientThread> clientThreads = new ArrayList<>(numClientes);
        for (int i = id * numClientes; i < (id + 1) * numClientes; i++) {
            clientThreads.add(new CoinClientThread(i, chave, users, tokens, numPartitions, percGlobal, percWrite));
        }
        // executar as threads.
        for (var t : clientThreads) {
            t.start();
        }
    }
}






















