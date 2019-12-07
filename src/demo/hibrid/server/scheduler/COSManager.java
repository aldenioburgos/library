package demo.hibrid.server.scheduler;

import demo.hibrid.server.ServerCommand;
import demo.hibrid.server.graph.ConflictDefinition;
import demo.hibrid.server.graph.LockFreeGraph;

public class COSManager {

    private LockFreeGraph<ServerCommand>[] graphs;

    public COSManager(int numGraphs, int maxCOSSize, ConflictDefinition<ServerCommand> conflictDefinition) {
        this.graphs = new LockFreeGraph[numGraphs];
        for (int i = 0; i < numGraphs; i++) {
            this.graphs[i] = new LockFreeGraph<>(maxCOSSize, conflictDefinition);
        }
    }

    public ServerCommand getFrom(int preferentialPartition) throws InterruptedException {
        //TODO aqui devo tentar primeiro na partição solicitada, se ela estiver vazia, tento nas outras, uma a uma, se todas estiverem vazias, eu me travo esperando a partição informada.
        return graphs[preferentialPartition].get();
    }

    public void addTo(int partitionToInsertTheCommand, ServerCommand serverCommand) throws InterruptedException {
        var partitions = serverCommand.getPartitions();
        for (int i = 0; i < partitions.length; i++) {
            var partition = partitions[i];
            if (partition == partitionToInsertTheCommand) {
                graphs[partition].insert(serverCommand); //TODO continuar daqui.
            } else {
                graphs[partition].inserDependencies(serverCommand);
            }
        }
    }

}
