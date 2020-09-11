package demo.hibrid.server.graph;

import demo.hibrid.server.CommandEnvelope;
import demo.hibrid.stats.Stats;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.Semaphore;

public class COSManager {

    private final Semaphore ready;
    private final Semaphore space;
    public final COS[] graphs;
    public final BlockingQueue<LockFreeNode> readyQueue = new LinkedTransferQueue<>();

    public COSManager(int numGraphs, int maxCOSSize, ConflictDefinition<CommandEnvelope> conflictDefinition) {
        assert numGraphs > 0 : "Invalid Argument numGraphs <= 0";
        assert maxCOSSize > 0 : "Invalid Argument maxCOSSize <= 0";
        assert conflictDefinition != null : "Invalid Argument conflictDefinition == null";

        this.ready = new Semaphore(0);
        this.space = new Semaphore(maxCOSSize);
        this.graphs = new COS[numGraphs];
        for (int i = 0; i < numGraphs; i++) {
            this.graphs[i] = new COS(i, conflictDefinition, this);
        }
    }


    public void acquireReady() {
        try {
            ready.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void releaseReady() {
        this.ready.release();
    }

    public void releaseSpace() {
        this.space.release();
    }

    public void acquireSpace() {
        assert Stats.cosSize(space.availablePermits()): "DEBUG";
        try {
            space.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public String toString() {
        return "COSManager{" +
                " ready=" + ready.availablePermits() +
                ", space=" + space.availablePermits() +
                ", graphs=" + Arrays.toString(graphs) +
                '}';
    }

    public void addToReadyQueue(LockFreeNode it) {
        readyQueue.add(it);
    }
}

