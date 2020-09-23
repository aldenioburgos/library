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
        Integer[] selectedPartitions = selectPartitions(rand, numPartitions, percTransacoesGlobais);
        Integer[] selectedIndexes = selectIndexes(rand, selectedPartitions.length, tamLista);
        int operationType = isWriteOperation(rand, percEscritas) ? Command.ADD : Command.GET;

        return new Command(operationType, selectedPartitions, selectedIndexes);
    }

    private static Integer[] selectPartitions(Random rand, int numPartitions, int percTransacoesGlobais) {
        int selector = rand.nextInt(100);
        if (selector < percTransacoesGlobais) {
            return allPartitions(numPartitions);
        } else {
            return selectPartition(rand, numPartitions);
        }
    }

    private static Integer[] selectIndexes(Random rand, int numSelectedPartitions, int tamLista) {
        Integer[] indexes = new Integer[numSelectedPartitions];
        for (int i = 0; i < numSelectedPartitions; i++) {
            indexes[i] = rand.nextInt(tamLista);
        }
        return indexes;
    }

    private static Integer[] allPartitions(int numPartitions) {
        Integer[] partitions = new Integer[numPartitions];
        for (int i = 0; i < numPartitions; i++) {
            partitions[i] = i;
        }
        return partitions;
    }

    private static Integer[] selectPartition(Random rand, int numPartitions) {
        Integer[] partition = new Integer[1];
        partition[0] = rand.nextInt(numPartitions);
        return partition;
    }


    private static boolean isWriteOperation(Random rand, int percWriteOperation) {
        int selector = rand.nextInt(100);
        return selector < percWriteOperation;
    }
}
