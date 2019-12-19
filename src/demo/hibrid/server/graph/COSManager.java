package demo.hibrid.server.graph;

import demo.hibrid.server.ServerCommand;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Semaphore;

public class COSManager {

    private Semaphore ready;
    private Semaphore space;
    private COS<ServerCommand>[] graphs;

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

    public ServerCommand get(int preferentialPartition) throws InterruptedException {
        assert preferentialPartition >= 0 : "Invalid Argument preferentialPartition < 0 ";
        assert preferentialPartition < graphs.length : "Invalid Argument preferentialPartition >= graphs.length";

        ready.acquire();
        int i = 0;
        Optional<ServerCommand> optServerCommand = Optional.empty();
        while (optServerCommand.isEmpty()) {
            var choosenPartition = (preferentialPartition + i++) % graphs.length;
            optServerCommand = graphs[choosenPartition].tryGet();
        }
        return optServerCommand.get();
    }


    public void remove(ServerCommand serverCommand) {
        assert serverCommand != null : "Invalid Argument serverCommand = null";

        var node = serverCommand.getNode();
        node.markRemoved();
        this.space.release();
    }


    public void addTo(int partitionToInsertTheCommand, ServerCommand serverCommand) throws InterruptedException {
        assert Arrays.stream(serverCommand.distinctPartitions).anyMatch(it -> it == partitionToInsertTheCommand) : "A partição para inserção do comando não está dentre as partições do comando.";

        this.space.acquire();
        var cos = graphs[partitionToInsertTheCommand];
        var node = cos.createNode(serverCommand);
        serverCommand.setNode(node);
        for (int partition : serverCommand.distinctPartitions) {
            graphs[partition].insertDependencies(node);
        }
        cos.insert(node);
    }

    void release() {
        this.ready.release();
    }

    public int getNumCOS() {
        return this.graphs.length;
    }

    @Override
    public String toString() {
        return "COSManager{" +
                " ready=" + ready.availablePermits() +
                ", graphs = " + Arrays.toString(graphs) +
                '}';
    }
}
