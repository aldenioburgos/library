package demo.parallelism;

import demo.hibrid.request.Command;

import java.util.Random;

public class CommandCreator {

    public static Command[] createCommands(int numeroOperacoes,
                                     int percTransacoesGlobais,
                                     int percEscritas,
                                     int numParticoes,
                                     int tamLista) {
        Random rand = new Random();
        Command[] commands = new Command[numeroOperacoes];
        for (int i = 0; i < commands.length; i++) {
            commands[i] = createListCommand(rand, numParticoes, percTransacoesGlobais, tamLista, percEscritas);
        }
        return commands;
    }


    private static Command createListCommand(Random rand, int numPartitions, int percTransacoesGlobais, int tamLista, int percEscritas) {
        int[] selectedPartitions = selectPartitions(rand, numPartitions, percTransacoesGlobais);
        int[] selectedIndexes = selectIndexes(rand, selectedPartitions.length, tamLista);
        int operationType = isWriteOperation(rand, percEscritas) ? Command.ADD : Command.GET;

        return new Command(operationType, selectedPartitions, selectedIndexes);
    }

    private static int[] selectPartitions(Random rand, int numPartitions, int percTransacoesGlobais) {
        int selector = rand.nextInt(100);
        if (selector < percTransacoesGlobais) {
            return allPartitions(numPartitions);
        } else {
            return selectPartition(rand, numPartitions);
        }
    }

    private static int[] selectIndexes(Random rand, int numSelectedPartitions, int tamLista) {
        int[] indexes = new int[numSelectedPartitions];
        for (int i = 0; i < numSelectedPartitions; i++) {
            indexes[i] = rand.nextInt(tamLista);
        }
        return indexes;
    }

    private static int[] allPartitions(int numPartitions) {
        int[] partitions = new int[numPartitions];
        for (int i = 0; i < numPartitions; i++) {
            partitions[i] = i;
        }
        return partitions;
    }

    private static int[] selectPartition(Random rand, int numPartitions) {
        int[] partition = new int[1];
        partition[0] = rand.nextInt(numPartitions);
        return partition;
    }


    private static boolean isWriteOperation(Random rand, int percWriteOperation) {
        int selector = rand.nextInt(100);
        return selector < percWriteOperation;
    }
}
