package demo.hibrid.server.graph;


import demo.hibrid.server.CommandEnvelope;
import demo.util.Utils.Action;

import java.util.Arrays;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static demo.util.Utils.fillWith;


public class LockFreeNode {
    /*
     NEW -> INSERTED -> READY -> REMOVED
     */
    public static final int NEW = 0;
    public static final int INSERTED = 1;
    public static final int READY = 2;
    public static final int COMPLETED = 3;
    public final AtomicInteger status;
    public final CommandEnvelope commandEnvelope;

    public final Edge[] listeners;
    public final LongAdder dependencies = new LongAdder();
    public final ReentrantReadWriteLock.WriteLock writeLock;
    public final ReentrantReadWriteLock.ReadLock readLock;

    public LockFreeNode(CommandEnvelope commandEnvelope, int numPartitions) {
        var lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
        this.listeners = new Edge[numPartitions];
        fillWith(listeners, Edge::new);
        this.commandEnvelope = commandEnvelope;
        this.status = new AtomicInteger(NEW);
    }


    public boolean isReady(){
        return (dependencies.intValue() == 0 && status.compareAndSet(INSERTED, READY));
    }

    @Override
    public String toString() {
        return "{status=" + status +
                ", data=" + commandEnvelope +
                ", dependencies=" + dependencies +
                ", listeners=" + Arrays.toString(listeners) +
                '}';
    }

    public static class Edge {
        public final LockFreeNode node;
        public final AtomicReference<Edge> nextEdge = new AtomicReference<>();

        public Edge() {
            node = null;
        }

        public Edge(LockFreeNode node) {
            this.node = node;
        }

        @Override
        public String toString() {
            return ((node == null) ? "[" : node.commandEnvelope.command.id) + ((nextEdge.get() == null) ? "]" : ", " + nextEdge);
        }

        public void forEach(Action<LockFreeNode> action){
            if (this.node != null) throw new UnsupportedOperationException("Método forEach só pode ser executado no head da lista!");
            Edge aux = this.nextEdge.get();
            while (aux != null) {
                action.apply(aux.node);
                aux = aux.nextEdge.get();
            }
        }

        public void insert(LockFreeNode newNode) {
            if (this.node != null) throw new UnsupportedOperationException("Método insert só deve ser executado no head da lista!");
            Edge aux = this;
            while (aux.node != newNode && (aux.nextEdge.get() != null || !aux.nextEdge.compareAndSet(null, new Edge(newNode)))) {
                aux = aux.nextEdge.get();
            }
        }
    }
}

