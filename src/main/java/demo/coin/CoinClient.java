package demo.coin;

import bftsmart.tom.ParallelServiceProxy;
import demo.coin.core.transactions.*;
import demo.coin.core.transactions.Transfer.ContaValor;
import demo.coin.util.ByteArray;
import demo.coin.util.ByteUtils;
import demo.coin.util.CryptoUtil;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import static demo.coin.CoinClient.PARAMS.*;

public class CoinClient {

    enum PARAMS {
        NUM_THREADS_CLIENTES, NUM_USUARIOS, NUM_TOTAL_OPERACOES, NUM_OPS_PER_REQ, NUM_PARTICOES, PERC_GLOBAL, PERC_WRITE, ROOT_PUBLIC_KEY, ROOT_PRIVATE_KEY, INIT_BALANCE
    }


    public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        //@formatter:off
        if (args.length != PARAMS.values().length) throw new IllegalArgumentException("Modo de uso:  java CoinClient NUM_THREADS_CLIENTES NUM_USUARIOS NUM_TOTAL_OPERACOES NUM_OPS_PER_REQ NUM_PARTICOES PERC_GLOBAL PERC_WRITE ROOT_PUBLIC_KEY ROOT_PRIVATE_KEY INIT_BALANCE");
        //@formatter:on

        int    numClientes   = Integer.parseInt(args[NUM_THREADS_CLIENTES.ordinal()]);
        int    numUsuarios   = Integer.parseInt(args[NUM_USUARIOS.ordinal()]);
        int    numOperacoes  = Integer.parseInt(args[NUM_TOTAL_OPERACOES.ordinal()]);
        int    numOperPerReq = Integer.parseInt(args[NUM_OPS_PER_REQ.ordinal()]);
        int    numParticoes  = Integer.parseInt(args[NUM_PARTICOES.ordinal()]);
        int    percGlobal    = Integer.parseInt(args[PERC_GLOBAL.ordinal()]);
        int    percWrite     = Integer.parseInt(args[PERC_WRITE.ordinal()]);
        long   initBalance   = Long.parseLong(args[INIT_BALANCE.ordinal()]);
        byte[] rootPubKey    = ByteUtils.convertToByteArray(args[ROOT_PUBLIC_KEY.ordinal()]);
        byte[] rootPriKey    = ByteUtils.convertToByteArray(args[ROOT_PRIVATE_KEY.ordinal()]);


        KeyPair rootKeys = new KeyPair(CryptoUtil.loadPublicKey(rootPubKey), CryptoUtil.loadPrivateKey(rootPriKey));
        // 1. criar os usuários
        Set<KeyPair> users = createUsers(numUsuarios);
        // 2. criar as threads clientes
        List<CoinClientThread> threads = createClientThreads(numClientes, numOperPerReq, numParticoes);
        // 3. criar operações de setup
        List<CoinOperation> setupOperations = createSetupOperations(rootKeys, users, numParticoes, initBalance);
        // 4. fazer o setup do sistema
        setup(setupOperations, threads);
        // 5. criar as operações
        List<CoinOperation> operations = createOperations(numOperacoes, numParticoes, percGlobal, percWrite, users);
        // 6. carregar as threads
        loadOperations(operations, threads);
        // 7. executar as threads.
        for (var t : threads) {
            t.start();
        }
    }

    private static List<CoinOperation> createOperations(int numOperacoes, int numParticoes, int percGlobal, int percWrite, Set<KeyPair> users) {
        Random              random    = new Random();
        List<CoinOperation> operacoes = new ArrayList<>(numOperacoes);

        for (int i = 0; i < numOperacoes; i++) {
            // 1 seleciono as particoes
            int[] particoes = selectParticoes(percGlobal, numParticoes);
            // 2. seleciono os usuarios.
            List<KeyPair> usuarios = selectUsuarios(users, 2);
            // 3. crio a operação
            operacoes.add(createOperation(usuarios, particoes, percWrite));
        }
        return operacoes;
    }

    private static CoinOperation createOperation(List<KeyPair> usuarios, int[] particoes, int percWrite) {
    //TODO esse modelo de sistema de moedas com utxo não serve para funcionar pre-carregado.
    // o dinheiro se movimenta entre os usuários dinamicamente.
    // o thread client vai ter que sortear n clientes e mandar esses fazerem operaçoes para os outros clientes da pasta.
    // o thread client deve armazenar as transações executadas com sucesso num localState, como se fossem utxos, assim é possivel
    // movimentar o dinheiro pra la e pra ca.

//        Random  random  = new Random();
//        boolean isWrite = random.nextInt(100) < percWrite;
//        if (isWrite) {
//            if (particoes.length > 1 && Arrays.stream(particoes).distinct().count() == particoes.length) {
//                return new Exchange(usuarios.get(0),particoes[0], )
//            } else {
//
//            }
//        } else {
//            return new Balance(usuarios.get(0), particoes);
//        }
        throw new UnsupportedOperationException("Not implemented");
    }

    private static List<KeyPair> selectUsuarios(Set<KeyPair> users, int numUsuariosToSelect) {
        Random        random   = new Random();
        List<KeyPair> usuarios = new ArrayList<>();
        List<KeyPair> lstUsers = new ArrayList<>(users);
        for (int i = 0; i < numUsuariosToSelect; i++) {
            usuarios.add(lstUsers.get(random.nextInt(lstUsers.size())));
        }
        return usuarios;
    }

    private static int[] selectParticoes(int percGlobal, int numParticoes) {
        Random random  = new Random();
        int    partSrc = random.nextInt(numParticoes);
        int    partDst = partSrc;

        // 1. local ou global? Quantas partições envolvidas?
        boolean isGlobal = random.nextInt(100) < percGlobal;
        while (isGlobal && partDst == partSrc) {
            partDst = random.nextInt(numParticoes);
        }
        return new int[]{partSrc, partDst};
    }


    private static List<CoinOperation> createSetupOperations(KeyPair rootKeys, Set<KeyPair> users, int numParticoes, long initBalance) {
        // registra os usuarios
        RegisterUsers registerUsers = new RegisterUsers(rootKeys, users);

        // cria os dinheiros
        long       mintAmount = users.size() * initBalance;
        List<Mint> mints      = new ArrayList<>(numParticoes);
        for (int i = 0; i < numParticoes; i++) {
            mints.add(new Mint(rootKeys, i, mintAmount));
        }

        // espalha os dinheiros
        List<Transfer> transfers = new ArrayList<>(numParticoes * users.size());
        for (int i = 0; i < numParticoes; i++) {
            long          remaining       = mintAmount;
            CoinOperation lastTransaction = mints.get(i);
            for (var user : users) {
                var transfer = new Transfer(rootKeys, i, Map.of(lastTransaction, 0), List.of(new ContaValor(user, initBalance), new ContaValor(rootKeys, remaining)));
                transfers.add(transfer);
                lastTransaction = transfer;
                remaining -= initBalance;
            }
        }

        List<CoinOperation> setupOperations = new ArrayList<>(transfers.size() + mints.size() + 1);
        setupOperations.add(registerUsers);
        setupOperations.addAll(mints);
        setupOperations.addAll(transfers);
        return setupOperations;
    }


    private static List<CoinClientThread> createClientThreads(int numClientes, int numOperPerReq, int numParticoes) {
        List<CoinClientThread> clientThreads = new ArrayList<>(numClientes);
        for (int i = 0; i < numClientes; i++) {
            clientThreads.add(new CoinClientThread(i, numOperPerReq, numParticoes));
        }
        return clientThreads;
    }

    private static Set<KeyPair> createUsers(int numUsuarios) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        Set<KeyPair> users = new HashSet<>(numUsuarios);
        for (int i = 0; i < numUsuarios; i++) {
            users.add(CryptoUtil.generateKeyPair());
        }
        return users;
    }


    private static void loadOperations(List<CoinOperation> allOperations, List<CoinClientThread> threads) {
        List<CoinOperation>[] ops = new ArrayList[threads.size()];
        for (int i = 0; i < ops.length; i++) {
            ops[i] = new ArrayList<>();
        }
        for (int i = 0; i < allOperations.size(); i++) {
            ops[i % ops.length].add(allOperations.get(i));
        }
        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).setOperations(ops[i]);
        }
    }

    private static void setup(List<CoinOperation> setupOperations, List<CoinClientThread> threads) {
        threads.get(0).setOperations(setupOperations);
    }
}
