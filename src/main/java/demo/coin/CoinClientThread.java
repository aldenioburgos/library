package demo.coin;


import bftsmart.tom.ParallelServiceProxy;
import demo.coin.core.Utxo;
import demo.coin.core.transactions.Balance;
import demo.coin.core.transactions.CoinOperation;
import demo.coin.core.transactions.Exchange;
import demo.coin.core.transactions.Transfer;

import java.security.KeyPair;
import java.util.*;


public class CoinClientThread extends Thread {

    private final Random random = new Random();
    private final ParallelServiceProxy proxy;
    private final KeyPair[] users;
    private final Utxo[] utxos;
    private final int percGlobal;
    private final int percWrite;
    private final int numPartitions;
    private final int userId;
    private final KeyPair myUser;

    public CoinClientThread(int userId, int threadId, KeyPair[] users, Utxo[] utxos, int numPartitions, int percGlobal, int percWrite) {
        super("CoinClientThread-" + threadId + "-User-" + userId);
        this.proxy = new ParallelServiceProxy(threadId);
        this.userId = userId;
        this.users = users;
        this.utxos = utxos;
        this.numPartitions = numPartitions;
        this.percGlobal = percGlobal;
        this.percWrite = percWrite;
        this.myUser = users[userId];
    }


    @Override
    public void run() {
        var reqNum = 0;
        //noinspection InfiniteLoopStatement
        while (true) {
            boolean isGlobal = random.nextInt(100) < percGlobal;
            int sourcePartition = random.nextInt(numPartitions);
            int destinyPartition = isGlobal ? selectOtherRandom(numPartitions, sourcePartition) : sourcePartition;
            int groupId = getGroupId(isGlobal, sourcePartition, destinyPartition);
            var request = createOperation(sourcePartition, destinyPartition);
            System.out.println("A requisição tem "+request.toByteArray().length+" bytes.");
            proxy.invokeParallel(request.toByteArray(), groupId);
            reqNum++;
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
            int receiverId = selectOtherRandom(users.length, userId);
            var receiver = users[receiverId];
            var inputs = List.of(new Transfer.Input(utxo.getTransactionHash(), utxo.getOutputPosition()));
            if (sourceCoin == destinyCoin) {
                operation = new Transfer(myUser, sourceCoin, inputs, List.of(new Transfer.ContaValor(receiver, utxo.getValue())));
            } else {
                operation = new Exchange(myUser, sourceCoin, inputs, List.of(new Exchange.ContaValorMoeda(receiver, utxo.getValue(), destinyCoin)));
            }
        } else {
            operation = new Balance(myUser, sourceCoin, destinyCoin);
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






















