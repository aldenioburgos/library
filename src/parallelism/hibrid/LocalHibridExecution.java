package parallelism.hibrid;

import demo.hibrid.client.HibridClient;
import demo.hibrid.client.HibridClientConfig;
import demo.hibrid.server.HibridExecutor;
import demo.hibrid.server.HibridServiceReplica;
import demo.hibrid.server.ListExecutor;

import java.util.Arrays;

import static parallelism.hibrid.util.ThreadUtil.join;
import static parallelism.hibrid.util.ThreadUtil.start;


public class LocalHibridExecution {

    //Configurações comuns.
    private static final int NUM_PARTITIONS = 2;

    // Configurações só do cliente
    private static final int TEST_SIZE = 20000;
    private static final int NUM_OPERATIONS_PER_REQUEST = 50;
    private static final int[] PERCENTUAL_OPERATIONS_PER_PARTITION = new int[]{50, 50};
    private static final int[] PERCENTUAL_PARTITIONS_ENVOLVED_IN_EACH_OPERATION = new int[]{50, 50};
    private static final int[] WRITES_PER_PARTITION = new int[]{20, 20};

    // Configurações só do servidor
    private static final int MAX_COS_SIZE = 150;
    private static final int MAX_QUEUE_SIZE = 150;

    private static final int MIN_READ_TIME = 1;
    private static final int MAX_READ_TIME = 5;
    private static final int MIN_WRITE_TIME = 5;
    private static final int MAX_WRITE_TIME = 10;
    private static final int NUM_WORKERS = 2;

    public static void main(String[] args) {
        if (args.length == 7) {
            var numParticoes = Integer.valueOf(args[0]);
            var numWorkerThreads = Integer.valueOf(args[1]);
            var tamLista = Integer.valueOf(args[2]);
            var percTransacoesGlobais = Integer.valueOf(args[3]);
            var percEscritas = Integer.valueOf(args[4]);
            var numeroOperacoes = Integer.valueOf(args[5]);
            var tamParticoes = Integer.valueOf(args[6]);

            System.out.println("n early: "+numParticoes);
            System.out.println("n late: "+numWorkerThreads);

            new LocalHibridExecution(numParticoes, numWorkerThreads, tamLista, percTransacoesGlobais, percEscritas, numeroOperacoes, tamParticoes);
        } else {
            System.out.println("Modo de uso: java parallelism.hibrid.LocalHibridExecution <numParticoes> <numWorkerThreads> <tamListas> <percTransacoesGlobais> <percEscritas> <numOperacoes> <tamParticoes>");
        }
    }

    public LocalHibridExecution(Integer numParticoes,
                                Integer numWorkerThreads,
                                Integer tamLista,
                                Integer percTransacoesGlobais,
                                Integer percEscritas,
                                Integer numeroOperacoes,
                                Integer tamParticoes) {
        ListExecutor executor = new ListExecutor(tamLista, numParticoes);
        HibridServiceReplica replica = new HibridServiceReplicaMock(0, numeroOperacoes, tamParticoes, numParticoes, numWorkerThreads, executor);
        Thread[] threads = replica.start();
        System.out.println(Arrays.toString(threads));
        HibridClient client = createClient(tamLista, replica, numParticoes, percTransacoesGlobais, percEscritas, numeroOperacoes);
        join(start(client));
        join(threads);
    }

    private HibridClient createClient(Integer tamLista,
                                      HibridServiceReplica replica,
                                      Integer numParticoes,
                                      Integer percTransacoesGlobais,
                                      Integer percEscritas,
                                      Integer numeroOperacoes) {
        var percentualPartitionsEnvolvedInEachOperation = new int[numParticoes];
        percentualPartitionsEnvolvedInEachOperation[0] = 100 - percTransacoesGlobais;
        percentualPartitionsEnvolvedInEachOperation[numParticoes - 1] = 100;
        var writesPerPartition = new int[numParticoes];
        Arrays.fill(writesPerPartition, percEscritas);
        var config = new HibridClientConfig(tamLista, numeroOperacoes, Integer.MAX_VALUE, numParticoes, percentualPartitionsEnvolvedInEachOperation, writesPerPartition);
        return new HibridClient(config, replica);
    }


    public LocalHibridExecution() {
        HibridExecutor executor = new HibridExecutor(MIN_READ_TIME, MAX_READ_TIME, MIN_WRITE_TIME, MAX_WRITE_TIME);
        HibridServiceReplica replica = new HibridServiceReplicaMock(0, MAX_QUEUE_SIZE, MAX_COS_SIZE, NUM_PARTITIONS, NUM_WORKERS, executor);

        var threads = replica.start();
        var client = createClient(replica);
        join(start(client));
        join(threads);
    }


    private HibridClient createClient(HibridServiceReplica replica) {
        var config = new HibridClientConfig(TEST_SIZE, NUM_OPERATIONS_PER_REQUEST, NUM_PARTITIONS, PERCENTUAL_OPERATIONS_PER_PARTITION, PERCENTUAL_PARTITIONS_ENVOLVED_IN_EACH_OPERATION, WRITES_PER_PARTITION);
        return new HibridClient(config, replica);
    }

}
