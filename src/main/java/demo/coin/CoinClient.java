package demo.coin;

import demo.coin.core.Utxo;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

enum PARAMS {
    NUM_THREADS_CLIENTE, NUM_OPERACOES_PER_CLIENTE, NUM_OPS_PER_REQ, PERC_GLOBAL, PERC_WRITE, WARM_UP_FILE
}


public class CoinClient {

    public void run(int numClientes, int numOperacoesPorCliente, int numOperPerReq, int percGlobal, int percWrite, WarmUp warmUp) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        //@formatter:off
        if (numClientes <= 0)                       throw new IllegalArgumentException();
        if (numOperPerReq <= 0)                     throw new IllegalArgumentException();
        if (numOperacoesPorCliente <= 0)            throw new IllegalArgumentException();
        if (percGlobal < 0 || percGlobal > 100)     throw new IllegalArgumentException();
        if (percWrite < 0 || percWrite > 100)       throw new IllegalArgumentException();
        if (warmUp == null)                         throw new IllegalArgumentException();
        if (warmUp.tokens.size() < numClientes)     throw new IllegalArgumentException();

        KeyPair[] users = warmUp.users.toArray(KeyPair[]::new);
        Utxo[] tokens = warmUp.tokens.toArray(Utxo[]::new);
        int numPartitions = warmUp.numPartitions;
        //@formatter:on
        List<CoinClientThread> clientThreads = new ArrayList<>(numClientes);
        for (int i = 0; i < numClientes; i++) {
            clientThreads.add(new CoinClientThread(i, users, tokens[i], numPartitions, numOperacoesPorCliente, numOperPerReq, percGlobal, percWrite));
        }
        // executar as threads.
        for (var t : clientThreads) {
            t.start();
        }
    }

    public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException {
        //@formatter:off
        if (args.length != PARAMS.values().length) throw new IllegalArgumentException("Modo de uso:  java CoinClient NUM_THREADS_CLIENTE NUM_OPERACOES_PER_CLIENTE NUM_OPS_PER_REQ PERC_GLOBAL PERC_WRITE WARM_UP_FILE");
        //@formatter:on

        int numClientes = Integer.parseInt(args[PARAMS.NUM_THREADS_CLIENTE.ordinal()]);
        int numOperacoes = Integer.parseInt(args[PARAMS.NUM_OPERACOES_PER_CLIENTE.ordinal()]);
        int numOperPerReq = Integer.parseInt(args[PARAMS.NUM_OPS_PER_REQ.ordinal()]);
        int percGlobal = Integer.parseInt(args[PARAMS.PERC_GLOBAL.ordinal()]);
        int percWrite = Integer.parseInt(args[PARAMS.PERC_WRITE.ordinal()]);
        String warmUpFile = args[PARAMS.WARM_UP_FILE.ordinal()];


        System.out.println("CoinClient executado com os seguintes argumentos:");
        System.out.println("\tnumThreadsClientes = " + numClientes);
        System.out.println("\tnumOperacoesPorCliente = " + numOperacoes);
        System.out.println("\tnumOperacoesPorRequisicao = " + numOperPerReq);
        System.out.println("\tpercGlobal = " + percGlobal);
        System.out.println("\tpercWrite = " + percWrite);
        System.out.println("\twarm-up file = " + warmUpFile);
        WarmUp warmUp = WarmUp.loadFrom(warmUpFile);
        System.out.println("CoinClient loaded to send " + numClientes * numOperacoes + " commands in, at least, " + numClientes * numOperacoes / numOperPerReq + " requests!");

        CoinClient client = new CoinClient();
        client.run(numClientes, numOperacoes, numOperPerReq, percGlobal, percWrite, warmUp);
    }
}






















