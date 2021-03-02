package parallelism.hibrid.late;

import parallelism.late.ConflictDefinition;
import parallelism.late.graph.COS;

/**
 *
 * @author eduardo
 */
public abstract class MultiPartitionCOS extends COS{


    public MultiPartitionCOS(int limit, ConflictDefinition cd, int partitions) {
        super(limit, cd);
    }
    
    @Override
     public void insert(Object request) throws InterruptedException {
        space.acquire();
        int readyNum =
                COSInsert(request);
        this.ready.release(readyNum);
    }
    
    @Override
    public void remove(Object requestNode) throws InterruptedException {
        int readyNum = 
                COSRemove(requestNode);
        this.space.release();
        this.ready.release(readyNum);
    }

    public Object get(int partition) throws InterruptedException {
        this.ready.acquire();
        return COSGet(partition);
    }
    

    @Override
    protected Object COSGet() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
     
    
    protected abstract Object COSGet(int partition) throws InterruptedException;

    
}
