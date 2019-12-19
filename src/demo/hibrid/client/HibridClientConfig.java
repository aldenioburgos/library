package demo.hibrid.client;

import java.util.Arrays;

public class HibridClientConfig {
    public final int clientProcessId;
    public final int numThreads;
    public final int numOperations;
    public final int interval;
    public final int maxListIndex;
    public final int numOperationsPerRequest;
    public final int numPartitions;
    public final int[] percentualDistributionOfOperationsAmongPartition;
    public final int[] percentualOfPartitionsEnvolvedPerOperation;
    public final int[] percentualOfWritesPerPartition;

    public HibridClientConfig(int numOperations, int numOperationsPerRequest, int numPartitions, int[] percentualDistributionOfOperationsAmongPartition, int[] percentualOfPartitionsEnvolvedPerOperation, int[] percentualOfWritesPerPartition) {
        this(0, 1, numOperations, 0, numOperationsPerRequest, numPartitions, percentualDistributionOfOperationsAmongPartition, percentualOfPartitionsEnvolvedPerOperation, percentualOfWritesPerPartition);
    }

    public HibridClientConfig(int clientProcessId,
                              int numThreads,
                              int numOperations,
                              int interval,
                              int numOperationsPerRequest,
                              int numPartitions,
                              int[] percentualDistributionOfOperationsAmongPartition,
                              int[] percentualOfPartitionsEnvolvedPerOperation,
                              int[] percentualOfWritesPerPartition) {
        this.maxListIndex = Integer.MAX_VALUE;
        this.clientProcessId = clientProcessId;
        this.numThreads = numThreads;
        this.numOperations = numOperations;
        this.interval = interval;
        this.numOperationsPerRequest = numOperationsPerRequest;
        this.numPartitions = numPartitions;
        this.percentualDistributionOfOperationsAmongPartition = pileValues(percentualDistributionOfOperationsAmongPartition);
        this.percentualOfPartitionsEnvolvedPerOperation = pileValues(percentualOfPartitionsEnvolvedPerOperation);
        this.percentualOfWritesPerPartition = percentualOfWritesPerPartition;
        assert selfValidation();
    }

    private boolean selfValidation() {
        assert numThreads > 0 : "Invalid Argument numThreads.";
        assert numOperations > 0 : "Invalid Argument numOperations.";
        assert interval >= 0 : "Invalid Argument interval.";
        assert numOperationsPerRequest > 0 : "Invalid Argument numOperationsPerRequest.";
        assert numPartitions > 0 : "Invalid Argument numPartitions.";
        assert percentualDistributionOfOperationsAmongPartition[percentualDistributionOfOperationsAmongPartition.length - 1] == 100 : "Invalid Argument percentualDistributionOfOperationsAmongPartition não soma 100%";
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
}
