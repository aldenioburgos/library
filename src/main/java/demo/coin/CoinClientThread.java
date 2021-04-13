package demo.coin;


import bftsmart.tom.ParallelServiceProxy;
import bftsmart.util.MultiOperationResponse;
import demo.coin.core.Utxo;
import demo.coin.core.requestresponse.CoinMultiOperationRequest;
import demo.coin.core.transactions.Balance;
import demo.coin.core.transactions.CoinOperation;
import demo.coin.core.transactions.Exchange;
import demo.coin.core.transactions.Transfer;

import java.security.KeyPair;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;


public class CoinClientThread extends Thread {

    private final Random random = new Random();
    private final ParallelServiceProxy proxy;
    private final KeyPair[] users;
    private final Utxo[] utxos;
    private final int percGlobal;
    private final int percWrite;
    private final int numPartitions;
    private final int id;

    public CoinClientThread(int id, KeyPair[] users, Utxo[] utxos, int numPartitions,  int percGlobal, int percWrite, ParallelServiceProxy proxy) {
        super("CoinClientThread-" + id);
        this.id = id;
        this.proxy = proxy;
        this.users = users;
        this.utxos = utxos;
        this.numPartitions = numPartitions;
        this.percGlobal = percGlobal;
        this.percWrite = percWrite;
    }

    @Override
    public void run() {
        while (true) {
            boolean isGlobal = random.nextInt(100) < percGlobal;
            int sourcePartition = random.nextInt(numPartitions);
            int destinyPartition = isGlobal ? selectOtherRandom(numPartitions, sourcePartition) : sourcePartition;
            int groupId = getGroupId(isGlobal, sourcePartition, destinyPartition);
            var request = new CoinMultiOperationRequest(createOperation(sourcePartition, destinyPartition));
            var bytes = proxy.invokeParallel(request.serialize(), groupId);
            System.out.println(request);
            System.out.println(new MultiOperationResponse(bytes));
        }
    }

    private int getGroupId(boolean isGlobal, int sourcePartition, int destinyPartition) {
        Set<Integer> partitions = isGlobal ? Set.of(sourcePartition, destinyPartition) : Set.of(sourcePartition);
        return new TreeSet<>(partitions).toString().hashCode();
    }

    private CoinOperation createOperation(int sourceCoin, int destinyCoin) {
        CoinOperation operation;
        var utxo = utxos[random.nextInt(utxos.length)];
        boolean isWrite = random.nextInt(100) < percWrite;
        if (isWrite) {
            int receiver = selectOtherRandom(users.length, id);
            var inputs = List.of(new Transfer.Input(utxo.getTransactionHash(), utxo.getOutputPosition()));
            if (sourceCoin == destinyCoin) {
                operation = new Transfer(users[id], sourceCoin, inputs, List.of(new Transfer.ContaValor(users[receiver], utxo.getValue())));
            } else {
                operation = new Exchange(users[id], sourceCoin, inputs, List.of(new Exchange.ContaValorMoeda(users[receiver], utxo.getValue(), destinyCoin)));
            }
        } else {
            operation = new Balance(users[id], sourceCoin, destinyCoin);
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





















