package demo.hibrid.client;

import demo.hibrid.request.Command;
import demo.hibrid.request.CommandResult;
import demo.hibrid.request.Request;

import java.util.Arrays;
import java.util.Random;

/**
 * Example client
 */
public class HibridClient extends Thread {

    private final BftAdapter bftAdapter;
    private final Random rand = new Random();
    private final HibridClientConfig config;

    private HibridClient(HibridClientConfig config, BftAdapter bftAdapter) {
        super("ClientWorker-" + config.clientProcessId);
        this.config = config;
        this.bftAdapter = bftAdapter;
    }

    public static HibridClient createClient(HibridClientConfig config, BftAdapter bftAdapter) {
        var proxy = (bftAdapter == null) ? new BftAdapter(0): bftAdapter;
        return new HibridClient(config, proxy);
    }



    @Override
    public void run() {
        System.out.println("Cliente "+config.clientProcessId+" tem "+config.numOperations+" operações para enviar.");
        for (int i = 0; i < config.numOperations; i += config.numOperationsPerRequest) {
            int numOpToExecute = Math.min(config.numOperations - i, config.numOperationsPerRequest);
            var commands = new Command[numOpToExecute];
            for (int j = 0; j < commands.length; j++) {
                int numPartitionsEnvolved = numPartitionsEnvolved();
                int[] selectedPartitions = selectPartitions(numPartitionsEnvolved);
                int[] selectedIndexes = selectIndexes(selectedPartitions);
                commands[j] = new Command(isWriteOperation(selectedPartitions) ? Command.ADD : Command.GET, selectedPartitions, selectedIndexes);
            }
            var request = new Request(config.clientProcessId, config.clientProcessId, commands);
            System.out.println("Cliente " + config.clientProcessId + " mandando a requisição " + request.getId() + " com " + request.getCommands().length + " comandos.");
            CommandResult[] result = bftAdapter.execute(request);
            System.out.println("Cliente " + config.clientProcessId + " recebeu resposta para requisição " + request.getId() + "= " + Arrays.toString(result));
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


}

