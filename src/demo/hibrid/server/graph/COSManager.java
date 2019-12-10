package demo.hibrid.server.graph;

import demo.hibrid.server.ServerCommand;

import java.util.Arrays;

public class COSManager {

    private HibridCOS<ServerCommand>[] graphs;

    public COSManager(int numGraphs, int maxCOSSize, ConflictDefinition<ServerCommand> conflictDefinition) {
        assert numGraphs > 0 : "Invalid Argument numGraphs = "+numGraphs;
        assert maxCOSSize > 0 : "Invalid Argument maxCOSSize = "+maxCOSSize;
        assert conflictDefinition != null : "Invalid Argument conflictDefinition = null";

        this.graphs = new HibridCOS[numGraphs];
        for (int i = 0; i < numGraphs; i++) {
            this.graphs[i] = new HibridCOS<>(maxCOSSize, conflictDefinition);
        }
    }

    public ServerCommand get(int preferentialPartition) throws InterruptedException {
        assert Thread.currentThread().getName().startsWith("HibridServiceReplicaWorker") : "COSManager.get() foi chamado pela thread " + Thread.currentThread().getName();
        assert preferentialPartition >= 0 : "Invalid Argument preferentialPartition = " + preferentialPartition;
        assert preferentialPartition < graphs.length : "Invalid Argument preferentialPartition = " + preferentialPartition + " >= graphs.length = " + graphs.length;

        COSNode<ServerCommand> node = null;
        for (int i = 0; i < graphs.length && node == null; i++) {
            node = graphs[(preferentialPartition + i) % graphs.length].tryGet();
        }
        if (node == null) {
            node = graphs[preferentialPartition].get();
        }
        return node.data;
    }


    public void remove(ServerCommand serverCommand) throws InterruptedException {
        assert Thread.currentThread().getName().startsWith("HibridServiceReplicaWorker") : "COSManager.remove() foi chamado pela thread " + Thread.currentThread().getName();
        assert serverCommand != null : "Invalid Argument serverCommand = null";

        var node = serverCommand.getNode();
        var cos = node.cos;

        synchronized (this) { //TODO remover depois.
            cos.remove(node);
            System.out.println(this);//TODO remover depois.
        }
    }


    public void addTo(int partitionToInsertTheCommand, ServerCommand serverCommand) throws InterruptedException {
        assert Thread.currentThread().getName().startsWith("LateScheduler") : "COSManager.addTo() foi chamado pela thread " + Thread.currentThread().getName();
        assert Arrays.stream(serverCommand.partitions).anyMatch(it -> it == partitionToInsertTheCommand) : "A partição para inserção do comando não está dentre as partições do comando.";

        var cos = graphs[partitionToInsertTheCommand];
        var node = cos.createNode(serverCommand);
        serverCommand.setNode(node);
        for (int partition : serverCommand.distinctPartitions) {
            graphs[partition].addDependencies(node);
        }
        synchronized (this) { //TODO remover depois.
            cos.insert(node);
            node.checkIfReady();
            System.out.println(this);//TODO remover depois.
        }
    }

    @Override
    public String toString() {
        return "COSManager{" +
                "graphs=" + Arrays.toString(graphs) +
                '}';
    }
}
