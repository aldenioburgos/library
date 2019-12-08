package demo.hibrid.server.graph;

import demo.hibrid.server.ServerCommand;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

public class COSManager {

    private HibridCOS<ServerCommand>[] graphs;
    private Map<ServerCommand, COSNode<ServerCommand>> reservedNodes = new Hashtable<>(); //TODO avaliar se não haveria outra solução, esse hashtable aqui é sincronizado entre as replicaworkers, pode criar um gargalo.


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
        assert preferentialPartition > 0 : "Invalid Argument preferentialPartition = " + preferentialPartition;
        assert preferentialPartition < graphs.length : "Invalid Argument preferentialPartition = " + preferentialPartition + " >= graphs.length = " + graphs.length;

        COSNode<ServerCommand> node = null;
        for (int i = 0; i < graphs.length && node == null; i++) {
            node = graphs[(preferentialPartition + i) % graphs.length].tryGet();
        }
        if (node == null) {
            node = graphs[preferentialPartition].get();
        }
        reservedNodes.put(node.data, node);
        return node.data;
    }


    public void remove(ServerCommand serverCommand) throws InterruptedException {
        assert Thread.currentThread().getName().startsWith("HibridServiceReplicaWorker") : "COSManager.remove() foi chamado pela thread " + Thread.currentThread().getName();
        assert serverCommand != null : "Invalid Argument serverCommand = null";

        var node = reservedNodes.remove(serverCommand);
        var cos = node.cos;
        cos.remove(node);
    }


    public void addTo(int partitionToInsertTheCommand, ServerCommand serverCommand) throws InterruptedException {
        assert Thread.currentThread().getName().startsWith("LateScheduler") : "COSManager.addTo() foi chamado pela thread " + Thread.currentThread().getName();
        assert Arrays.stream(serverCommand.getPartitions()).anyMatch(it -> it == partitionToInsertTheCommand) : "A partição para inserção do comando não está dentre as partições do comando.";

        var cos = graphs[partitionToInsertTheCommand];
        var node = cos.createNode(serverCommand);
        var partitions = serverCommand.getPartitions();
        for (int i = 0; i < partitions.length; i++) {
            var partition = partitions[i];
            graphs[partition].addDependencies(node);
        }
        cos.insert(node);
        node.checkIfReady();
    }

}
