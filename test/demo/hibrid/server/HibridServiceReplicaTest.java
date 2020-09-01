package demo.hibrid.server;

import demo.hibrid.client.HibridClient;
import demo.hibrid.client.HibridClientConfig;

import static demo.hibrid.server.util.ThreadUtil.join;
import static demo.hibrid.server.util.ThreadUtil.start;

public class HibridServiceReplicaTest  {

    //Configurações comuns.
    private static final int NUM_PARTITIONS = 2;

    // Configurações só do cliente
    private static final int TEST_SIZE = 20000;
    private static final int NUM_OPERATIONS_PER_REQUEST = 50;
    private static final int[] PERCENTUAL_OPERATIONS_PER_PARTITION = new int[]{50, 50};
    private static final int[] PERCENTUAL_PARTITIONS_ENVOLVED_IN_EACH_OPERATION = new int[]{50, 50};
    private static final int[] WRITES_PER_PARTITION = new int[]{20, 20};

    // Configurações só do servidor
    private static final int MIN_READ_TIME = 1;
    private static final int MAX_READ_TIME = 5;
    private static final int MIN_WRITE_TIME = 5;
    private static final int MAX_WRITE_TIME = 10;
    private static final int MAX_QUEUE_SIZE = 100;
    private static final int MAX_COS_SIZE = 150;
    private static final int NUM_WORKERS = 2;


    public static void main(String[] args) {
        new HibridServiceReplicaTest();
    }


    public HibridServiceReplicaTest() {
        HibridExecutor executor = new HibridExecutor(MIN_READ_TIME, MAX_READ_TIME, MIN_WRITE_TIME, MAX_WRITE_TIME);
        HibridServiceReplica replica = new HibridServiceReplicaMock(0, MAX_QUEUE_SIZE, MAX_COS_SIZE, NUM_PARTITIONS, NUM_WORKERS, executor);

        var threads = replica.start();
        var client  = createClient(replica);
        System.out.println(client);
        join(start(client));
        join(threads);
    }


    private HibridClient createClient(HibridServiceReplica replica) {
        var config = new HibridClientConfig(TEST_SIZE, NUM_OPERATIONS_PER_REQUEST, NUM_PARTITIONS, PERCENTUAL_OPERATIONS_PER_PARTITION, PERCENTUAL_PARTITIONS_ENVOLVED_IN_EACH_OPERATION, WRITES_PER_PARTITION);
        return new HibridClient(config, replica);
    }

}
