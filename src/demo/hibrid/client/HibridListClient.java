package demo.hibrid.client;

import demo.hibrid.request.Command;

import java.io.IOException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Example client
 */
public class HibridListClient {
    private boolean stop = false;
    private HibridClientConfig config;
    private ServerProxyFactory serverProxyFactory;

    public HibridListClient(HibridClientConfig config, ServerProxyFactory serverProxyFactory) {
        this.config = config;
        this.serverProxyFactory = serverProxyFactory;
    }

    public void start() throws InterruptedException, IOException {
        (new Timer()).schedule(new TimerTask() {
            public void run() {
                stop = false;
            }
        }, 5 * 60000); //depois de 5 minutos encerra.

        AccountClientWorker[] workers = createClientWorkers();
        Thread.sleep(300); //TODO pra que esse tempo aqui mesmo?
        for (AccountClientWorker worker : workers) {
            worker.start();
        }
        for (AccountClientWorker worker : workers) {
            worker.join();
        }
    }

    private AccountClientWorker[] createClientWorkers() throws InterruptedException {
        var workers = new AccountClientWorker[config.numThreads];
        for (int i = 0; i < config.numThreads; i++) {
            Thread.sleep(100); //TODO pra que esse tempo aqui mesmo?
            workers[i] = new AccountClientWorker(i, calcNumRequestsForWorker(i), serverProxyFactory.getServerProxy(i));
        }
        return workers;
    }

    private int calcNumRequestsForWorker(int i) {
        int exactDivision = config.numOperations / config.numThreads;
        int remainder = config.numOperations % config.numThreads;
        if (remainder != 0 && i == 0) {
            return exactDivision + remainder;
        } else {
            return exactDivision;
        }
    }


    public class AccountClientWorker extends Thread {
        private final int numOperations;
        private final int id;
        private final ServerProxy server;
        private final Random rand = new Random();

        AccountClientWorker(int id, int numberOfRqs, ServerProxy server) {
            super("ClientWorker" + id);
            this.id = id;
            this.numOperations = numberOfRqs;
            this.server = server;
        }

        @Override
        public void run() {
            for (int i = 0; i < numOperations && !stop; i += config.numOperationsPerRequest) {
                int numOpToExecute = Math.min(numOperations - i, config.numOperationsPerRequest);
                var operations = new Command[numOpToExecute];
                for (int j = 0; j < operations.length; j++) {
                    int numPartitionsEnvolved = numPartitionsEnvolved();
                    int[] selectedPartitions = selectPartitions(numPartitionsEnvolved);
                    int[] selectedIndexes = selectIndexes(selectedPartitions);
                    operations[j] = new Command(isWriteOperation(selectedPartitions) ? Command.ADD : Command.GET, selectedPartitions, selectedIndexes);
                }
                try {
                    server.execute(config.clientProcessId, id, operations);
                    if (config.interval > 0) {
                        Thread.sleep(config.interval);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
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


    }
}
