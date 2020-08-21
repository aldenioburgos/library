package demo.hibrid.server.graph;

import demo.hibrid.server.ServerCommand;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Semaphore;

public class COSManager {

    private final Semaphore ready;
    private final Semaphore space;
    private final COS<ServerCommand>[] graphs;

    public COSManager(int numGraphs, int maxCOSSize, ConflictDefinition<ServerCommand> conflictDefinition) {
        assert numGraphs > 0 : "Invalid Argument numGraphs <= 0";
        assert maxCOSSize > 0 : "Invalid Argument maxCOSSize <= 0";
        assert conflictDefinition != null : "Invalid Argument conflictDefinition == null";

        this.ready = new Semaphore(0);
        this.space = new Semaphore(maxCOSSize);
        this.graphs = new COS[numGraphs];
        for (int i = 0; i < numGraphs; i++) {
            this.graphs[i] = new COS<>(conflictDefinition, this);
        }
    }

    public ServerCommand get(int preferentialPartition){
        assert preferentialPartition >= 0 : "Invalid Argument preferentialPartition < 0 ";
        assert preferentialPartition < graphs.length : "Invalid Argument preferentialPartition >= graphs.length";

        Optional<ServerCommand> optServerCommand;
        int i = 0;
        System.out.println("Vou pegar, agora existem " + ready.availablePermits() + " comandos disponíveis no COS.");
        acquireReady();
        do {
            var choosenPartition = (preferentialPartition + i++) % graphs.length;
            optServerCommand = graphs[choosenPartition].tryGet();
        } while (optServerCommand.isEmpty());
        return optServerCommand.get();
    }

    private void acquireReady() {
        try {
            ready.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void releaseReady() {
        this.ready.release();
    }



    public void addTo(int partitionToInsertTheCommand, ServerCommand serverCommand) throws InterruptedException {
        assert Arrays.stream(serverCommand.distinctPartitions).anyMatch(it -> it == partitionToInsertTheCommand) : "A partição para inserção do comando não está dentre as partições do comando.";

        this.space.acquire();
        COS<ServerCommand> cos = graphs[partitionToInsertTheCommand];
        LockFreeNode<ServerCommand> node = cos.createNode(serverCommand);
        for (int partition : serverCommand.distinctPartitions) {
            graphs[partition].insertDependencies(node);
        }
        serverCommand.setNode(node);
        cos.insert(node);
        System.out.println(this);
    }

    public int getNumCOS() {
        return this.graphs.length;
    }

    @Override
    public String toString() {
        return "COSManager{" +
                " ready=" + ready.availablePermits() +
                ", space=" + space.availablePermits() +
                ", graphs = " + Arrays.toString(graphs) +
                '}';
    }

    public void remove(ServerCommand serverCommand) {
        serverCommand.getNode().markRemoved();
        space.release();
    }
}
