package demo.hibrid.server.graph;

import demo.hibrid.server.CommandEnvelope;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Semaphore;

public class COSManager {

    private final Semaphore ready;
    private final Semaphore space;
    public final COS[] graphs;

    public COSManager(int numGraphs, int maxCOSSize, ConflictDefinition<CommandEnvelope> conflictDefinition) {
        assert numGraphs > 0 : "Invalid Argument numGraphs <= 0";
        assert maxCOSSize > 0 : "Invalid Argument maxCOSSize <= 0";
        assert conflictDefinition != null : "Invalid Argument conflictDefinition == null";

        this.ready = new Semaphore(0);
        this.space = new Semaphore(maxCOSSize);
        this.graphs = new COS[numGraphs];
        for (int i = 0; i < numGraphs; i++) {
            this.graphs[i] = new COS(conflictDefinition, this);
        }
    }

    public CommandEnvelope getFrom(int preferentialPartition){
        assert preferentialPartition >= 0 : "Invalid Argument preferentialPartition < 0 ";
        assert preferentialPartition < graphs.length : "Invalid Argument preferentialPartition >= graphs.length";

        Optional<CommandEnvelope> optServerCommand;
        int i = 0;
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

    void acquireSpace() {
        try {
            space.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void releaseSpace() {
        this.space.release();
    }


    public void remove(CommandEnvelope commandEnvelope) {
        commandEnvelope.getNode().markRemoved();
        space.release();
    }

    @Override
    public String toString() {
        return "COSManager{" +
                " ready=" + ready.availablePermits() +
                ", space=" + space.availablePermits() +
                ", graphs = " + Arrays.toString(graphs) +
                '}';
    }
}
