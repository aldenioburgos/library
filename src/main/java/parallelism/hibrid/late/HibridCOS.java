package parallelism.hibrid.late;

import parallelism.ParallelMapping;
import parallelism.late.ConflictDefinition;

public class HibridCOS extends MultiPartitionCOS {

    private ExtendedLockFreeGraph[] subgraphs;
    private ParallelMapping pm;

    public HibridCOS(int limit, ConflictDefinition cd, int numSubgraphs, ParallelMapping pm) {
        super(limit, cd, numSubgraphs);
        this.subgraphs = new ExtendedLockFreeGraph[numSubgraphs];
        for (int i = 0; i < numSubgraphs; i++) {
            this.subgraphs[i] = new ExtendedLockFreeGraph(cd, i, 150);
        }
        this.pm = pm;
    }

    @Override
    protected Object COSGet(int partition) throws InterruptedException {
        int start = partition;
        Object ret = null;

        while (ret == null) {
            ret = this.subgraphs[start % this.subgraphs.length].get();
            start++;
        }
        return ret;
    }


    @Override
    protected int COSInsert(Object request) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int COSRemove(Object request) {
        throw new UnsupportedOperationException();
    }


}
