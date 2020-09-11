package demo.hibrid.server.graph;

import demo.hibrid.server.CommandEnvelope;
import demo.hibrid.stats.Stats;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TransferQueue;

public class COSManager {

    private final Semaphore space;
    private final ConflictDefinition<CommandEnvelope> conflictDefinition;
    public final TransferQueue<LockFreeNode> readyQueue = new LinkedTransferQueue<>();
    public final TransferQueue<LockFreeNode> completedQueue = new LinkedTransferQueue<>();

    public COSManager(int numGraphs, int maxCOSSize, ConflictDefinition<CommandEnvelope> conflictDefinition) {
        assert numGraphs > 0 : "Invalid Argument numGraphs <= 0";
        assert maxCOSSize > 0 : "Invalid Argument maxCOSSize <= 0";
        assert conflictDefinition != null : "Invalid Argument conflictDefinition == null";
        this.conflictDefinition = conflictDefinition;
        this.space = new Semaphore(maxCOSSize);
    }

    public void releaseSpace() {
        this.space.release();
    }

    public void acquireSpace() {
        assert Stats.cosSize(150-space.availablePermits()) : "DEBUG";
        try {
            space.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public String toString() {
        return "COSManager{" +
                ", space=" + space.availablePermits() +
                '}';
    }

    public void addToReadyQueue(LockFreeNode it) {
        readyQueue.add(it);
    }


    public boolean isDependent(CommandEnvelope commandEnvelope, CommandEnvelope commandEnvelope1) {
        return conflictDefinition.isDependent(commandEnvelope, commandEnvelope1);
    }
}

