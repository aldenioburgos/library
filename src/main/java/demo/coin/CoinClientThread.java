package demo.coin;


import bftsmart.tom.ParallelServiceProxy;
import demo.coin.core.requestresponse.CoinMultiOperationRequest;
import demo.coin.core.transactions.CoinOperation;
import demo.coin.early.CoinHibridClassToThreads;

import java.util.*;


public class CoinClientThread extends Thread {

    private int id;
    private final int numOpsPerRequest;
    private final ParallelServiceProxy proxy;

    private List<CoinOperation> allOperations;
    private byte[][]            responses;

    private final Map<Integer, List<CoinOperation>> operationsPerGroupId;
    private final Set<SortedSet<Integer>>           allPartitionArrangements;


    public CoinClientThread(int id, int numOpsPerRequest, int numPartitions, List<CoinOperation> allOperations) {
        this.id = id;
        this.numOpsPerRequest = numOpsPerRequest;
        this.proxy = new ParallelServiceProxy(id);
        this.operationsPerGroupId = new HashMap<>();
        this.allPartitionArrangements = CoinHibridClassToThreads.getAllArrangements(numPartitions);
        this.allOperations = allOperations;
        this.responses = new byte[allOperations.size()][];
        splitOperationsPerGroupId();
    }

    @Override
    public void run() {
        for (var opGroup : operationsPerGroupId.entrySet()) {
            var multiRequest = new CoinMultiOperationRequest();
            int index        = 0;
            for (CoinOperation operation : opGroup.getValue()) {
                multiRequest.add(operation);
                if (++index > numOpsPerRequest) {
                    proxy.invokeParallel(multiRequest.serialize(), opGroup.getKey());
                    multiRequest = new CoinMultiOperationRequest();
                    index = 0;
                }
            }
            if (index > 0) {
                proxy.invokeParallel(multiRequest.serialize(), opGroup.getKey());
            }
        }
        printResponses();
    }

    private void splitOperationsPerGroupId() {
        for (var op : allOperations) {
            int groupId = op.getClassId(allPartitionArrangements);
            if (!operationsPerGroupId.containsKey(groupId)) {
                operationsPerGroupId.put(groupId, new ArrayList<>());
            }
            operationsPerGroupId.get(groupId).add(op);
        }
    }

    public byte[] setUp(List<CoinOperation> setupOperations){
        return proxy.invokeParallel(new CoinMultiOperationRequest(setupOperations).serialize(), 0);
    }

    public void printResponses(){
        for (int i = 0; i < responses.length; i++) {
            System.out.println(id + " - " + allOperations.get(i)+ " -> " + Arrays.toString(responses[i]));
        }
    }
}






















