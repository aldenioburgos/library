package demo.hibrid.client;

import bftsmart.tom.ServiceProxy;
import demo.hibrid.request.Command;
import demo.hibrid.request.CommandResult;
import demo.hibrid.request.Request;
import demo.hibrid.server.HibridServiceReplica;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Example client
 */
public class HibridClient extends Thread {

    private final HibridServiceReplica replica;
    private final ServiceProxy proxy;
    private final Random rand = new Random();
    private final HibridClientConfig config;

    public HibridClient(HibridClientConfig config) {
        this(config, null, new ServiceProxy(config.clientProcessId));
    }

    public HibridClient(HibridClientConfig config, HibridServiceReplica replica) {
        this(config, replica, null);
    }

    private HibridClient(HibridClientConfig config, HibridServiceReplica replica, ServiceProxy proxy) {
        super("ClientWorker-" + config.clientProcessId);
        this.replica = replica;
        this.proxy = proxy;
        this.config = config;
    }

    @Override
    public void run() {
        var requests = new ArrayList<Request>((config.numOperations / config.numOperationsPerRequest) + 1);
        for (int i = 0; i < config.numOperations; i += config.numOperationsPerRequest) {
            int numOpToExecute = Math.min(config.numOperations - i, config.numOperationsPerRequest);
            var commands = new Command[numOpToExecute];
            for (int j = 0; j < commands.length; j++) {
                int numPartitionsEnvolved = numPartitionsEnvolved();
                int[] selectedPartitions = selectPartitions(numPartitionsEnvolved);
                int[] selectedIndexes = selectIndexes(selectedPartitions);
                commands[j] = new Command(isWriteOperation(selectedPartitions) ? Command.ADD : Command.GET, selectedPartitions, selectedIndexes);
            }
            requests.add(new Request(config.clientProcessId, config.clientProcessId, commands));
        }

        if (replica != null) {
            System.out.println("Start:"+System.nanoTime());
            for (var request : requests) {
                replica.processRequest(request);
            }
        } else {
            for (var request : requests) {
                byte[] results = proxy.invokeOrdered(request.toBytes());
                System.out.println(new CommandResult().fromBytes(results));
            }
        }
    }


    private boolean isWriteOperation(int[] selectedPartitions) {
        var result = true;
        var seletor = rand.nextInt(100);
        for (int selectedPartition : selectedPartitions) {
            if (config.percentualOfWritesPerPartition[selectedPartition] <= seletor) {
                result = false;
                break;
            }
        }
        return result;
    }

    private int[] selectIndexes(int[] selectedPartitions) {
        assert selectedPartitions.length > 0 : "Deve haver ao menos uma partição selecionada.";
        var indexes = new int[selectedPartitions.length];
        for (int i = 0; i < selectedPartitions.length; i++) {
            indexes[i] = selectIndex(selectedPartitions[i]);
        }
        return indexes;
    }

    private int selectIndex(int partition) {
        int division = config.maxListIndex / config.numPartitions;
        int remainder = config.maxListIndex % config.numPartitions;
        if (remainder == 0 || partition < (config.numPartitions - 1)) {
            return rand.nextInt(division) + (partition * division);
        } else {
            return rand.nextInt(division + remainder) + (partition * division);
        }
    }

    private int numPartitionsEnvolved() {
        var selector = rand.nextInt(100);
        for (int i = 0; i < config.percentualOfPartitionsEnvolvedPerOperation.length; i++) {
            if (selector < config.percentualOfPartitionsEnvolvedPerOperation[i]) {
                return i + 1; // a posição x se refere ao percentual de operações com x+1 partição envolvida;
            }
        }
        throw new RuntimeException("Sempre deve haver ao menos uma partição envolvida.");
    }

    private int[] selectPartitions(int numPartitionsEnvolved) {
        var partitions = new int[numPartitionsEnvolved];
        for (int i = 0; i < partitions.length; i++) {
            partitions[i] = selectPartition();
        }
        return partitions;
    }

    private int selectPartition() {
        var selector = rand.nextInt(100);
        for (int i = 0; i < config.percentualDistributionOfOperationsAmongPartition.length; i++) {
            if (selector < config.percentualDistributionOfOperationsAmongPartition[i]) {
                return i;
            }
        }
        throw new RuntimeException("Sempre deve haver ao menos uma partição envolvida.");
    }

    @Override
    public String toString() {
        return "HibridClient{" +
                "replica=" + replica +
                ", proxy=" + proxy +
                ", rand=" + rand +
                ", config=" + config +
                '}';
    }
}

