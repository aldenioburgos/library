package demo.coin;

import demo.coin.core.Utxo;
import demo.coin.core.transactions.*;
import demo.coin.core.transactions.Exchange.ContaValorMoeda;
import demo.coin.core.transactions.Transfer.ContaValor;
import demo.coin.util.Pair;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

enum PARAMS {
    NUM_THREADS_CLIENTE, NUM_OPERACOES_PER_CLIENTE, NUM_OPS_PER_REQ, NUM_PARTICOES, PERC_GLOBAL, PERC_WRITE, WARM_UP_FILE
}


public class CoinClient {

    public void run(int numClientes, int numOperacoesPorCliente, int numOperPerReq, int numParticoes, int percGlobal, int percWrite, WarmUp warmUp) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        //@formatter:off
        if (numClientes <= 0)                   throw new IllegalArgumentException();
        if (numOperacoesPorCliente <= 0)        throw new IllegalArgumentException();
        if (numParticoes <= 0)                  throw new IllegalArgumentException();
        if (percGlobal < 0 || percGlobal > 100) throw new IllegalArgumentException();
        if (percWrite < 0 || percWrite > 100)   throw new IllegalArgumentException();
        if (numOperacoesPorCliente * percWrite / 100 > warmUp.tokens.size() * numParticoes * warmUp.users.size() / numClientes) throw new IllegalArgumentException();
        //@formatter:on

        Map<KeyPair, Set<Utxo>>[] partialState = createPartialState(warmUp);

        // criar as threads clientes
        List<CoinClientThread> clientThreads = new ArrayList<>(numClientes);
        for (int i = 0; i < numClientes; i++) {
            // Criar as operações
            List<CoinOperation> operations = createClientOperations(partialState, numOperacoesPorCliente, numParticoes, percGlobal, percWrite);
            clientThreads.add(new CoinClientThread(i, numOperPerReq, numParticoes, operations));
        }
        // executar as threads.
        for (var t : clientThreads) {
            t.start();
        }
    }

    private List<CoinOperation> createClientOperations(Map<KeyPair, Set<Utxo>>[] partialState, int numOperacoes, int numParticoes, int percGlobal, int percWrite) {
        var random = new Random();
        var users = new ArrayList<>(partialState[0].keySet());
        var usersIterator = users.iterator();
        Pair<KeyPair, KeyPair> selectedUsers = null;
        Set<CoinOperation> operacoes = new HashSet<>(numOperacoes);
        while (operacoes.size() < numOperacoes) {
            try {
                // 1 seleciono as particoes
                int[] particoes = selectPartitions(percGlobal, numParticoes);
                //@formatter:off seleciono os usuarios.
                if (!usersIterator.hasNext()) usersIterator = users.iterator();
                do selectedUsers = new Pair<>(usersIterator.next(), users.get(random.nextInt(users.size())));
                while (selectedUsers.a.equals(selectedUsers.b));
                //@formatter:on
                // crio a operação
                operacoes.add(createOperation(selectedUsers, particoes, percWrite, partialState));
            } catch (UsuarioLisoException e) {
                // não faz nada
            }
        }
        return new ArrayList<>(operacoes);
    }

    private Map<KeyPair, Set<Utxo>>[] createPartialState(WarmUp warmUp) {
        Map<KeyPair, Set<Utxo>>[] partialState = new Map[warmUp.numPartitions];
        for (int i = 0; i < warmUp.numPartitions; i++) {
            partialState[i] = new HashMap<>(warmUp.users.size());
            for (KeyPair user : warmUp.users) {
                partialState[i].put(user, new HashSet<>(warmUp.tokens));
            }
        }
        return partialState;
    }

    protected CoinOperation createOperation(Pair<KeyPair, KeyPair> users, int[] partitions, int percWrite, Map<KeyPair, Set<Utxo>>[] partialState) {
        Random random = new Random();
        KeyPair sender = users.a;
        KeyPair receiver = users.b;
        int sourceCoin = partitions[0];
        int destinyCoin = partitions[1];
        CoinOperation newOperation;

        boolean isLocal = sourceCoin == destinyCoin;
        boolean isWrite = random.nextInt(100) < percWrite;
        if (isWrite) {
            var utxos = partialState[sourceCoin].get(sender);
            if (utxos.isEmpty()) {
                throw new UsuarioLisoException();
            }
            var utxo = utxos.stream().findFirst().get();
            utxos.remove(utxo); // consume input transaction
            var inputs = List.of(new Transfer.Input(utxo.getTransactionHash(), utxo.getOutputPosition()));
            newOperation = (isLocal) ? new Transfer(sender, sourceCoin, inputs, List.of(new ContaValor(receiver, utxo.getValue()))) : new Exchange(sender, sourceCoin, inputs, List.of(new ContaValorMoeda(receiver, utxo.getValue(), destinyCoin)));
        } else {
            newOperation = new Balance(sender, partitions);
        }
        return newOperation;
    }


    protected int[] selectPartitions(int percGlobal, int numParticoes) {
        //@formatter:off
        if (percGlobal < 0 || percGlobal > 100) throw new IllegalArgumentException();
        if (numParticoes <= 0) throw new IllegalArgumentException();
        //@formatter:on
        Random random = new Random();
        int partSrc = random.nextInt(numParticoes);
        int partDst = partSrc;

        // 1. local ou global? Quantas partições envolvidas?
        boolean isGlobal = random.nextInt(100) < percGlobal;
        while (isGlobal && partDst == partSrc) {
            partDst = random.nextInt(numParticoes);
        }
        return new int[]{partSrc, partDst};
    }


    static class UsuarioLisoException extends RuntimeException {}

    public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException {
        //@formatter:off
        if (args.length != PARAMS.values().length) throw new IllegalArgumentException("Modo de uso:  java CoinClient NUM_THREADS_CLIENTE NUM_OPERACOES_PER_CLIENTE NUM_OPS_PER_REQ NUM_PARTICOES PERC_GLOBAL PERC_WRITE WARM_UP_FILE");
        //@formatter:on
        int numClientes = Integer.parseInt(args[PARAMS.NUM_THREADS_CLIENTE.ordinal()]);
        int numOperacoes = Integer.parseInt(args[PARAMS.NUM_OPERACOES_PER_CLIENTE.ordinal()]);
        int numOperPerReq = Integer.parseInt(args[PARAMS.NUM_OPS_PER_REQ.ordinal()]);
        int numParticoes = Integer.parseInt(args[PARAMS.NUM_PARTICOES.ordinal()]);
        int percGlobal = Integer.parseInt(args[PARAMS.PERC_GLOBAL.ordinal()]);
        int percWrite = Integer.parseInt(args[PARAMS.PERC_WRITE.ordinal()]);
        String warmUpFile = args[PARAMS.WARM_UP_FILE.ordinal()];

        System.out.println("Loading warm-up file...");
        WarmUp warmUp = WarmUp.loadFrom(warmUpFile);
        System.out.println("Warm-up file loaded!");

        CoinClient client = new CoinClient();
        client.run(numClientes, numOperacoes, numOperPerReq, numParticoes, percGlobal, percWrite, warmUp);
    }
}






















