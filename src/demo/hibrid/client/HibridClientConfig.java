package demo.hibrid.client;

import java.util.Arrays;

public class HibridClientConfig {
    public final int clientProcessId;
    public final int numThreads;
    public final int numOperations;
    public final int maxListIndex;
    public final int numOperationsPerRequest;
    public final int numPartitions;
    public int[] percentualDistributionOfOperationsAmongPartition;
    public final int[] percentualOfPartitionsEnvolvedPerOperation;
    public final int[] percentualOfWritesPerPartition;

    public HibridClientConfig(int numOperations,
                              int numOperationsPerRequest,
                              int numPartitions,
                              int[] percentualDistributionOfOperationsAmongPartition,
                              int[] percentualOfPartitionsEnvolvedPerOperation,
                              int[] percentualOfWritesPerPartition) {
        this(Integer.MAX_VALUE, 0, 1, numOperations, numOperationsPerRequest, numPartitions, percentualDistributionOfOperationsAmongPartition, percentualOfPartitionsEnvolvedPerOperation, percentualOfWritesPerPartition);
    }

    public HibridClientConfig(int maxListIndex,
                              int numOperations,
                              int numOperationsPerRequest,
                              int numPartitions,
                              int[] percentualOfPartitionsEnvolvedPerOperation,
                              int[] percentualOfWritesPerPartition) {
        this(maxListIndex, 0, 1, numOperations, numOperationsPerRequest, numPartitions, null, percentualOfPartitionsEnvolvedPerOperation, percentualOfWritesPerPartition);
    }

    public HibridClientConfig(int maxListIndex,
                              int clientProcessId,
                              int numThreads,
                              int numOperations,
                              int numOperationsPerRequest,
                              int numPartitions,
                              int[] percentualDistributionOfOperationsAmongPartition,
                              int[] percentualOfPartitionsEnvolvedPerOperation,
                              int[] percentualOfWritesPerPartition) {
        this.maxListIndex = maxListIndex;
        this.clientProcessId = clientProcessId;
        this.numThreads = numThreads;
        this.numOperations = numOperations;
        this.numOperationsPerRequest = numOperationsPerRequest;
        this.numPartitions = numPartitions;
        this.percentualDistributionOfOperationsAmongPartition = (percentualDistributionOfOperationsAmongPartition == null) ? null : pileValues(percentualDistributionOfOperationsAmongPartition);
        this.percentualOfPartitionsEnvolvedPerOperation = pileValues(percentualOfPartitionsEnvolvedPerOperation);
        this.percentualOfWritesPerPartition = percentualOfWritesPerPartition;
        assert selfValidation();
    }

    private boolean selfValidation() {
        assert numThreads > 0 : "Invalid Argument numThreads.";
        assert numOperations > 0 : "Invalid Argument numOperations.";
        assert numOperationsPerRequest > 0 : "Invalid Argument numOperationsPerRequest.";
        assert numPartitions > 0 : "Invalid Argument numPartitions.";
        assert percentualDistributionOfOperationsAmongPartition == null || percentualDistributionOfOperationsAmongPartition[percentualDistributionOfOperationsAmongPartition.length - 1] == 100 : "Invalid Argument percentualDistributionOfOperationsAmongPartition não soma 100%";
        assert percentualOfPartitionsEnvolvedPerOperation[percentualOfPartitionsEnvolvedPerOperation.length - 1] == 100 : "Invalid Argument percentualOfPartitionsEnvolvedPerOperation não soma 100%";
        assert Arrays.stream(percentualOfWritesPerPartition).allMatch(it -> it >= 0) : "Invalid Argument percentualOfWritesPerPartition, existem um percentual negativo.";
        return true;
    }


    private int[] pileValues(int[] origin) {
        int[] array = Arrays.copyOf(origin, origin.length);
        for (int i = 1; i < array.length; i++) {
            array[i] = (array[i - 1] < 100) ? array[i - 1] + array[i] : 0;
        }
        return array;
    }

    @Override
    public String toString() {
        return "HibridClientConfig{" +
                "clientProcessId=" + clientProcessId +
                ", numThreads=" + numThreads +
                ", numOperations=" + numOperations +
                ", maxListIndex=" + maxListIndex +
                ", numOperationsPerRequest=" + numOperationsPerRequest +
                ", numPartitions=" + numPartitions +
                ", percentualDistributionOfOperationsAmongPartition=" + Arrays.toString(percentualDistributionOfOperationsAmongPartition) +
                ", percentualOfPartitionsEnvolvedPerOperation=" + Arrays.toString(percentualOfPartitionsEnvolvedPerOperation) +
                ", percentualOfWritesPerPartition=" + Arrays.toString(percentualOfWritesPerPartition) +
                '}';
    }
}
