package demo.coin;


import bftsmart.tom.ParallelServiceProxy;
import demo.coin.core.Utxo;
import demo.coin.core.requestresponse.CoinMultiOperationRequest;
import demo.coin.core.transactions.Balance;
import demo.coin.core.transactions.CoinOperation;
import demo.coin.core.transactions.Exchange;
import demo.coin.core.transactions.Transfer;

import java.security.KeyPair;
import java.util.*;


public class CoinClientThread extends Thread {

    private final ParallelServiceProxy proxy;
    private final int numOperPerReq;
    private final int percGlobal;
    private final int percWrite;
    private final KeyPair[] users;
    private final Utxo utxo;
    private final int numPartitions;
    private final Random random = new Random();
    // contador de operações remanescentes
    private int numOperacoes;

    public CoinClientThread(int id, KeyPair[] users, Utxo utxo, int numPartitions, int numOperacoes, int numOperPerReq, int percGlobal, int percWrite) {
        super("CoinClientThread-" + id);
        this.proxy = new ParallelServiceProxy(7001+id);
        this.users = users;
        this.utxo = utxo;
        this.numPartitions = numPartitions;
        this.numOperacoes = numOperacoes;
        this.numOperPerReq = numOperPerReq;
        this.percGlobal = percGlobal;
        this.percWrite = percWrite;
    }

    @Override
    public void run() {
        while (numOperacoes > 0) {
            boolean isGlobal = random.nextInt(100) < percGlobal;
            int sourcePartition = selectOtherRandom(numPartitions, -1);
            int destinyPartition = isGlobal ? selectOtherRandom(numPartitions, sourcePartition) : sourcePartition;
            Set<Integer> partitions = (isGlobal) ? Set.of(sourcePartition, destinyPartition) : Set.of(sourcePartition);
            int groupId = new TreeSet<>(partitions).toString().hashCode();
            CoinMultiOperationRequest request = crateRequest(sourcePartition, destinyPartition);
            proxy.invokeParallel(request.serialize(), groupId);
        }
    }

    private CoinMultiOperationRequest crateRequest(int from, int to) {
        // criar as operações
        List<CoinOperation> operations = new ArrayList<>(numOperPerReq);
        for (int i = 0; i < numOperPerReq && this.numOperacoes > 0; i++) {
            boolean isWrite = random.nextInt(100) < percWrite;
            operations.add(createOperation(isWrite, from, to));
            this.numOperacoes--;
        }

        return new CoinMultiOperationRequest(operations);
    }

    private CoinOperation createOperation(boolean isWrite, int sourceCoin, int destinyCoin) {
        CoinOperation operation;
        int sender = selectOtherRandom(users.length, -1);
        if (isWrite) {
            int receiver = selectOtherRandom(users.length, sender);
            var inputs = List.of(new Transfer.Input(utxo.getTransactionHash(), utxo.getOutputPosition()));
            if (sourceCoin == destinyCoin) {
                operation = new Transfer(users[sender], sourceCoin, inputs, List.of(new Transfer.ContaValor(users[receiver], utxo.getValue())));
            } else {
                operation = new Exchange(users[sender], sourceCoin, inputs, List.of(new Exchange.ContaValorMoeda(users[receiver], utxo.getValue(), destinyCoin)));
            }
        } else {
            operation = new Balance(users[sender], sourceCoin, destinyCoin);
        }
        return operation;
    }


    protected int selectOtherRandom(int numOptions, int firstRandom) {
        if (numOptions < 2) {
            throw new IllegalArgumentException();
        }
        int secondRandom = random.nextInt(numOptions);
        return (secondRandom != firstRandom) ? secondRandom : selectOtherRandom(numOptions, firstRandom);
    }
}






















