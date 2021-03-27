package demo.coin;

import demo.coin.core.CoinGlobalState;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class WarmUpService {

    enum PARAMS {
        NUM_USUARIOS, NUM_TOKENS_USUARIO,  NUM_PARTICOES

    }

    private int numUsuarios;
    private int numParticoes;
    private long initBalance;
    private CoinGlobalState coinGlobalState;


    public WarmUpService(int numUsuarios, int numParticoes, long initBalance) {
        this.numUsuarios = numUsuarios;
        this.numParticoes = numParticoes;
        this.initBalance = initBalance;
    }



    public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        //@formatter:off
        if (args.length != PARAMS.values().length) throw new IllegalArgumentException("Modo de uso:  java CoinClient NUM_THREADS_CLIENTE, NUM_USUARIOS_THREAD, NUM_OPERACOES_USUARIO, NUM_TOKENS_USUARIO, NUM_OPS_PER_REQ, NUM_PARTICOES, PERC_GLOBAL, PERC_WRITE, ROOT_PUBLIC_KEY, ROOT_PRIVATE_KEY");
        //@formatter:on

        int numUsuarios = Integer.parseInt(args[PARAMS.NUM_USUARIOS.ordinal()]);
        int numParticoes = Integer.parseInt(args[PARAMS.NUM_PARTICOES.ordinal()]);
        long initBalance = Long.parseLong(args[PARAMS.NUM_TOKENS_USUARIO.ordinal()]);
        WarmUpService warmUpService = new WarmUpService(numUsuarios, numParticoes, initBalance);
        warmUpService.run();
    }

    private void run() {
        //TODO terminar
    }


//
//
//    public void run() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
//        // cria os usuarios
//        System.out.println("Criando " + numUsuariosPorCliente * numClientes + " usuários.");
//        Set<KeyPair> users = createUsers(numUsuariosPorCliente * numClientes);
//        System.out.println("Registrando os usuários:");
//        Utils.groupBy(Utils.groupBy(users, 256).stream().map(it -> new RegisterUsers(rootKeys, it)).collect(Collectors.toSet()), numOperPerReq).forEach(this::send); //envia
//
//        // cria o dinheiro
//        System.out.println("Criando " + numClientes * numUsuariosPorCliente * initBalance + " tokens.");
//        List<Mint> mints = mintMoney(rootKeys, numParticoes, numClientes * numUsuariosPorCliente * initBalance);
//        System.out.println("Registrando os tokens:");
//        Utils.groupBy(mints, numOperPerReq).forEach(this::send); // envia
//
//        // espalha o dinheiro
//        List<Transfer> transfers = spreadMoney(rootKeys, users, mints);
//        System.out.println("Transferindo os tokens para os usuários:");
//        Utils.groupBy(transfers, numOperPerReq).forEach(this::send); // envia
//
//        // cria o estado parcial
//        partialState = createPartialState(users, transfers, numParticoes);
//        // criar as threads clientes
//        List<CoinClientThread> clientThreads = new ArrayList<>(numClientes);
//        // a thread 0 é a main thread!
//        for (int i = 0; i < numClientes; i++) {
//            // Criar as operações
//            var operations = createOperations(numOperPerUsuario * numUsuariosPorCliente, numParticoes, percGlobal, percWrite, users, partialState);
//            clientThreads.add(new CoinClientThread(i + 1, numOperPerReq, numParticoes, operations));
//        }
//
//        // executar as threads.
//        for (var t : clientThreads) {
//            t.start();
//        }
//    }
//
//
//    protected void send(Iterable<? extends CoinOperation> operations) {
//        if (proxy == null) {
//            proxy = new ParallelServiceProxy(0);
//        }
//        var request = new CoinMultiOperationRequest(operations);
//        var response = new CoinMultiOperationResponse(proxy.invokeParallel(request.serialize(), 0));
//        System.out.println("\t"+request);
//        System.out.println("\t"+response);
//    }
//
//    protected Map<KeyPair, Map<CoinOperation, Pair<Integer, Long>>>[] createPartialState(Set<KeyPair> users, List<Transfer> transfers, int numParticoes) {
//        Map<KeyPair, Map<CoinOperation, Pair<Integer, Long>>>[] partialState = new Map[numParticoes];
//        for (int i = 0; i < numParticoes; i++) {
//            partialState[i] = new HashMap<>(users.size());
//            for (var user : users) {
//                partialState[i].put(user, new HashMap<>());
//            }
//        }
//        for (var t : transfers) {
//            var outputs = t.getOutputs();
//            for (int i = 0; i < t.getOutputs().size(); i++) {
//                Transfer.Output output = outputs.get(i);
//                KeyPair receiverKeyPair = getUser(users, t.getUser(output.receiverAccountIndex));
//                partialState[t.getCurrency()].get(receiverKeyPair).put(t, new Pair<>(i, output.value));
//            }
//        }
//        return partialState;
//    }
//
//    protected KeyPair getUser(Set<KeyPair> users, byte[] user) {
//        for (var u : users) {
//            if (Arrays.equals(u.getPublic().getEncoded(), user)) {
//                return u;
//            }
//        }
//        throw new IllegalArgumentException();
//    }
//
//    protected List<CoinOperation> createOperations(int numOperacoes, int numParticoes, int percGlobal, int percWrite, Set<KeyPair> users,
//                                                   Map<KeyPair, Map<CoinOperation, Pair<Integer, Long>>>[] partialState) {
//        //@formatter:off
//            if (numOperacoes <= 0) throw new IllegalArgumentException();
//            if (numParticoes <= 0) throw new IllegalArgumentException();
//            if (percGlobal < 0 || percGlobal > 100) throw new IllegalArgumentException();
//            if (percWrite < 0 || percWrite > 100) throw new IllegalArgumentException();
//        //@formatter:on
//        Set<CoinOperation> operacoes = new HashSet<>(numOperacoes);
//        while (operacoes.size() < numOperacoes) {
//            try {
//                // 1 seleciono as particoes
//                int[] particoes = selectParticoes(percGlobal, numParticoes);
//                // 2. seleciono os usuarios.
//                List<KeyPair> usuarios = selectUsuarios(users, 2);
//                // 3. crio a operação
//                var operationPair = createOperation(usuarios, particoes, percWrite, partialState);
//                // 4. atualizo o estado do cliente
//                updatePartialState(users, operationPair, partialState);
//                // 5. armazeno a operação
//                operacoes.add(operationPair.b);
//            } catch (UsuarioLisoException e) {
//                // não faz nada
//            }
//        }
//        return new ArrayList<>(operacoes);
//    }
//
//    protected void updatePartialState(Set<KeyPair> users, Pair<? extends CoinOperation, ? extends CoinOperation> ops,
//                                      Map<KeyPair, Map<CoinOperation, Pair<Integer, Long>>>[] partialState) {
//        if (ops.b instanceof RegisterUsers || ops.b instanceof Balance)
//            return;
//        if (ops.b instanceof Transfer) {
//            KeyPair user = getUser(users, ops.b.getIssuer());
//            partialState[((Transfer) ops.b).getCurrency()].get(user).remove(ops.a);
//        }
//        if (ops.b instanceof Exchange) {
//            for (int i = 0; i < ((Exchange) ops.b).getOutputs().size(); i++) {
//                Output output = (Output) ((Exchange) ops.b).getOutputs().get(i);
//                KeyPair user = getUser(users, ops.b.getUser(output.receiverAccountIndex));
//                partialState[output.currency].get(user).put(ops.b, new Pair<>(i, output.value));
//            }
//        } else if (ops.b instanceof Transfer) {
//            for (int i = 0; i < ((Transfer) ops.b).getOutputs().size(); i++) {
//                Transfer.Output output = ((Transfer) ops.b).getOutputs().get(i);
//                KeyPair user = getUser(users, ops.b.getUser(output.receiverAccountIndex));
//                partialState[((Transfer) ops.b).getCurrency()].get(user).put(ops.b, new Pair<>(i, output.value));
//            }
//        }
//    }
//
//    protected Pair<? extends CoinOperation, ? extends CoinOperation> createOperation(List<KeyPair> usuarios, int[] particoes, int percWrite,
//                                                                                     Map<KeyPair, Map<CoinOperation, Pair<Integer, Long>>>[] partialState) {
//        KeyPair sender = usuarios.get(0);
//        KeyPair receiver = usuarios.get(1);
//        Random random = new Random();
//        boolean isWrite = random.nextInt(100) < percWrite;
//        CoinOperation inputOperation = null;
//        CoinOperation newOperation;
//        if (isWrite) {
//            var optInput = partialState[particoes[0]].get(sender).entrySet().stream().findFirst();
//            if (optInput.isEmpty()) {
//                throw new UsuarioLisoException();
//            }
//            var input = optInput.get();
//            inputOperation = input.getKey();
//            int inputOperationOutputIndex = input.getValue().a;
//            long inputValue = input.getValue().b;
//            int moedaOrigem = particoes[0];
//            // cambio
//            if (Arrays.stream(particoes).distinct().count() == particoes.length) {
//                int moedaDestino = particoes[1];
//                newOperation = new Exchange(sender, moedaOrigem, Map.of(inputOperation, inputOperationOutputIndex), List.of(new ContaValorMoeda(receiver, inputValue, moedaDestino)));
//            } else { // transferencia simples
//                newOperation = new Transfer(sender, moedaOrigem, Map.of(inputOperation, inputOperationOutputIndex), List.of(new ContaValor(receiver, inputValue)));
//            }
//        } else {
//            newOperation = new Balance(sender, particoes);
//        }
//        return new Pair<>(inputOperation, newOperation);
//    }
//
//    protected List<KeyPair> selectUsuarios(Set<KeyPair> users, int numUsuariosToSelect) {
//        //@formatter:off
//         if (numUsuariosToSelect < 0) throw new IllegalArgumentException();
//         if (users.size() < numUsuariosToSelect) throw new IllegalArgumentException();
//         if (users.isEmpty()) throw new IllegalArgumentException();
//         //@formatter:on
//        Random random = new Random();
//        List<KeyPair> usuarios = new ArrayList<>();
//        List<KeyPair> lstUsers = new ArrayList<>(users);
//        for (int i = 0; i < numUsuariosToSelect; i++) {
//            usuarios.add(lstUsers.remove(random.nextInt(lstUsers.size())));
//        }
//        return usuarios;
//    }
//
//    protected int[] selectParticoes(int percGlobal, int numParticoes) {
//        //@formatter:off
//        if (percGlobal < 0 || percGlobal > 100) throw new IllegalArgumentException();
//        if (numParticoes <= 0) throw new IllegalArgumentException();
//        //@formatter:on
//        Random random = new Random();
//        int partSrc = random.nextInt(numParticoes);
//        int partDst = partSrc;
//
//        // 1. local ou global? Quantas partições envolvidas?
//        boolean isGlobal = random.nextInt(100) < percGlobal;
//        while (isGlobal && partDst == partSrc) {
//            partDst = random.nextInt(numParticoes);
//        }
//        return new int[]{partSrc, partDst};
//    }
//
//    protected List<Transfer> spreadMoney(KeyPair rootKeys, Collection<KeyPair> users, List<Mint> mints) {
//        //@formatter:off
//            if (rootKeys == null) throw new IllegalArgumentException();
//            if (users == null || users.isEmpty()) throw new IllegalArgumentException();
//            if (mints == null || mints.isEmpty()) throw new IllegalArgumentException();
//        //@formatter:on
//        // espalha o dinheiro de cada partição
//        var lstUsers = new ArrayList<>(users);
//        List<Transfer> transfers = new ArrayList<>(mints.size());
//        int i = 0;
//        for (var mint : mints) {
//            transfers.add(new Transfer(rootKeys, mint.getCurrency(), Map.of(mint, 0), List.of(new ContaValor(lstUsers.get(i++ % lstUsers.size()), mint.getValue()))));
//        }
//        return transfers;
//    }
//
//
//    protected List<Mint> mintMoney(KeyPair rootKeys, int numParticoes, long mintAmount) {
//        List<Mint> mints = new ArrayList<>(numParticoes);
//        for (int i = 0; i < numParticoes; i++) {
//            for (int j = 0; j < mintAmount; j++) {
//                mints.add(new Mint(rootKeys, i, 1L));
//            }
//        }
//        return mints;
//    }
//
//
//    protected Set<KeyPair> createUsers(int numUsuarios) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
//        //@formatter:off
//        if (numUsuarios <= 0) throw new IllegalArgumentException();
//        //@formatter:on
//        Set<KeyPair> users = new HashSet<>(numUsuarios);
//        for (int i = 0; i < numUsuarios; i++) {
//            users.add(CryptoUtil.generateKeyPair());
//        }
//        return users;
//    }


    static class UsuarioLisoException extends RuntimeException {}
}
