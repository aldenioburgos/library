package demo.coin;

import demo.coin.core.Utxo;
import demo.coin.core.transactions.*;
import demo.coin.core.transactions.Exchange.ContaValorMoeda;
import demo.coin.core.transactions.Exchange.Output;
import demo.coin.core.transactions.Transfer.ContaValor;
import demo.coin.util.CryptoUtil;
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
        Set<CoinOperation> operacoes = new HashSet<>(numOperacoes);
        while (operacoes.size() < numOperacoes) {
            try {
                // 1 seleciono as particoes
                int[] particoes = selectParticoes(percGlobal, numParticoes);
                // 2. seleciono os usuarios.
                List<KeyPair> usuarios = selectUsuarios(users, 2);
                // 3. crio a operação
                var operationPair = createOperation(usuarios, particoes, percWrite, partialState);
                // 4. atualizo o estado do cliente
                updatePartialState(users, operationPair, partialState);
                // 5. armazeno a operação
                operacoes.add(operationPair.b);
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

    protected KeyPair getUser(Set<KeyPair> users, byte[] user) {
        for (var u : users) {
            if (Arrays.equals(u.getPublic().getEncoded(), user)) {
                return u;
            }
        }
        throw new IllegalArgumentException();
    }


    protected void updatePartialState(Set<KeyPair> users, Pair<? extends CoinOperation, ? extends CoinOperation> ops,
                                      Map<KeyPair, Map<CoinOperation, Pair<Integer, Long>>>[] partialState) {
        if (ops.b instanceof RegisterUsers || ops.b instanceof Balance)
            return;
        if (ops.b instanceof Transfer) {
            KeyPair user = getUser(users, ops.b.getIssuer());
            partialState[((Transfer) ops.b).getCurrency()].get(user).remove(ops.a);
        }
        if (ops.b instanceof Exchange) {
            for (int i = 0; i < ((Exchange) ops.b).getOutputs().size(); i++) {
                Output output = (Output) ((Exchange) ops.b).getOutputs().get(i);
                KeyPair user = getUser(users, ops.b.getUser(output.receiverAccountIndex));
                partialState[output.currency].get(user).put(ops.b, new Pair<>(i, output.value));
            }
        } else if (ops.b instanceof Transfer) {
            for (int i = 0; i < ((Transfer) ops.b).getOutputs().size(); i++) {
                Transfer.Output output = ((Transfer) ops.b).getOutputs().get(i);
                KeyPair user = getUser(users, ops.b.getUser(output.receiverAccountIndex));
                partialState[((Transfer) ops.b).getCurrency()].get(user).put(ops.b, new Pair<>(i, output.value));
            }
        }
    }

    protected Pair<? extends CoinOperation, ? extends CoinOperation> createOperation(List<KeyPair> usuarios, int[] particoes, int percWrite,
                                                                                     Map<KeyPair, Map<CoinOperation, Pair<Integer, Long>>>[] partialState) {
        KeyPair sender = usuarios.get(0);
        KeyPair receiver = usuarios.get(1);
        Random random = new Random();
        boolean isWrite = random.nextInt(100) < percWrite;
        CoinOperation inputOperation = null;
        CoinOperation newOperation;
        if (isWrite) {
            var optInput = partialState[particoes[0]].get(sender).entrySet().stream().findFirst();
            if (optInput.isEmpty()) {
                throw new UsuarioLisoException();
            }
            var input = optInput.get();
            inputOperation = input.getKey();
            int inputOperationOutputIndex = input.getValue().a;
            long inputValue = input.getValue().b;
            int moedaOrigem = particoes[0];
            // cambio
            if (Arrays.stream(particoes).distinct().count() == particoes.length) {
                int moedaDestino = particoes[1];
                newOperation = new Exchange(sender, moedaOrigem, Map.of(inputOperation, inputOperationOutputIndex), List.of(new ContaValorMoeda(receiver, inputValue, moedaDestino)));
            } else { // transferencia simples
                newOperation = new Transfer(sender, moedaOrigem, Map.of(inputOperation, inputOperationOutputIndex), List.of(new ContaValor(receiver, inputValue)));
            }
        } else {
            newOperation = new Balance(sender, particoes);
        }
        return new Pair<>(inputOperation, newOperation);
    }

    protected List<KeyPair> selectUsuarios(Set<KeyPair> users, int numUsuariosToSelect) {
        //@formatter:off
         if (numUsuariosToSelect < 0) throw new IllegalArgumentException();
         if (users.size() < numUsuariosToSelect) throw new IllegalArgumentException();
         if (users.isEmpty()) throw new IllegalArgumentException();
         //@formatter:on
        Random random = new Random();
        List<KeyPair> usuarios = new ArrayList<>();
        List<KeyPair> lstUsers = new ArrayList<>(users);
        for (int i = 0; i < numUsuariosToSelect; i++) {
            usuarios.add(lstUsers.remove(random.nextInt(lstUsers.size())));
        }
        return usuarios;
    }

    protected int[] selectParticoes(int percGlobal, int numParticoes) {
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

    protected List<Transfer> spreadMoney(KeyPair rootKeys, Collection<KeyPair> users, List<Mint> mints) {
        //@formatter:off
            if (rootKeys == null) throw new IllegalArgumentException();
            if (users == null || users.isEmpty()) throw new IllegalArgumentException();
            if (mints == null || mints.isEmpty()) throw new IllegalArgumentException();
        //@formatter:on
        // espalha o dinheiro de cada partição
        var lstUsers = new ArrayList<>(users);
        List<Transfer> transfers = new ArrayList<>(mints.size());
        int i = 0;
        for (var mint : mints) {
            transfers.add(new Transfer(rootKeys, mint.getCurrency(), Map.of(mint, 0), List.of(new ContaValor(lstUsers.get(i++ % lstUsers.size()), mint.getValue()))));
        }
        return transfers;
    }


    protected List<Mint> mintMoney(KeyPair rootKeys, int numParticoes, long mintAmount) {
        List<Mint> mints = new ArrayList<>(numParticoes);
        for (int i = 0; i < numParticoes; i++) {
            for (int j = 0; j < mintAmount; j++) {
                mints.add(new Mint(rootKeys, i, 1L));
            }
        }
        return mints;
    }


    protected Set<KeyPair> createUsers(int numUsuarios) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        //@formatter:off
        if (numUsuarios <= 0) throw new IllegalArgumentException();
        //@formatter:on
        Set<KeyPair> users = new HashSet<>(numUsuarios);
        for (int i = 0; i < numUsuarios; i++) {
            users.add(CryptoUtil.generateKeyPair());
        }
        return users;
    }


    static class UsuarioLisoException extends RuntimeException {
    }

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






















