package demo.hibrid.server.graph;

import java.util.concurrent.atomic.AtomicBoolean;

public class PaddedAtomicBoolean {
    public long a,b,c,d,e,f;
    public AtomicBoolean atomicBoolean;
    public long g,h,i,j,k,l;

    public PaddedAtomicBoolean(boolean value) {
        this.atomicBoolean = new AtomicBoolean(value);
    }

    public final boolean compareAndSet(boolean expectedValue, boolean newValue) {
        return atomicBoolean.compareAndSet(expectedValue,newValue);
    }

    public final boolean get() {
        return atomicBoolean.get();
    }

}
