package demo.coin;


import bftsmart.tom.ParallelServiceProxy;
import demo.coin.core.requestresponse.CoinMultiOperationRequest;
import demo.coin.core.requestresponse.CoinMultiOperationResponse;
import demo.coin.core.transactions.CoinOperation;
import demo.coin.early.CoinHibridClassToThreads;

import java.io.Console;
import java.util.*;


public class CoinClientThread extends Thread {

    private int id;
    private final int numOpsPerRequest;
    private final ParallelServiceProxy proxy;

    private List<CoinOperation> allOperations;
    private List<byte[]> responses;

    private final Map<Integer, List<CoinOperation>> operationsPerGroupId;
    private final Set<SortedSet<Integer>> allPartitionArrangements;


    public CoinClientThread(int id, int numOpsPerRequest, int numPartitions, List<CoinOperation> allOperations) {
        this.id = id;
        this.numOpsPerRequest = numOpsPerRequest;
        this.proxy = new ParallelServiceProxy(id);
        this.operationsPerGroupId = new HashMap<>();
        this.allPartitionArrangements = CoinHibridClassToThreads.getAllArrangements(numPartitions);
        this.allOperations = allOperations;
        this.responses = new ArrayList<>(allOperations.size());
        splitOperationsPerGroupId();
    }

    @Override
    public void run() {
        for (var opGroup : operationsPerGroupId.entrySet()) {
            int index = 0;
            var multiRequest = new CoinMultiOperationRequest();
            for (CoinOperation operation : opGroup.getValue()) {
                multiRequest.add(operation);
                if (++index > numOpsPerRequest) {
                    responses.add(proxy.invokeParallel(multiRequest.serialize(), opGroup.getKey()));
                    multiRequest = new CoinMultiOperationRequest();
                    index = 0;
                }
            }

            if (index > 0) {
                responses.add(proxy.invokeParallel(multiRequest.serialize(), opGroup.getKey()));
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

    public byte[] setUp(List<CoinOperation> setupOperations) {
        return proxy.invokeParallel(new CoinMultiOperationRequest(setupOperations).serialize(), 0);
    }

    public void printResponses() {
        responses.stream().map(CoinMultiOperationResponse::new).forEach(it -> System.out.println("Client Thread #"+id+"# ->"+it));
    }
}






















