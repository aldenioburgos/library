package demo.hibrid.server.graph;


import demo.hibrid.server.CommandEnvelope;
import demo.util.Utils;
import demo.util.Utils.Action;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static demo.util.Utils.fillWith;


public class LockFreeNode {
    /*
     NEW -> INSERTED -> READY -> RESERVED -> REMOVED
     */
    public static final int NEW = 0;
    public static final int INSERTED = 1;
    public static final int READY = 2;
    public static final int RESERVED = 3;
    public static final int REMOVED = 5;
    public final AtomicInteger status;
    public final CommandEnvelope commandEnvelope;
    public final COS cos;

    public final Edge[] listeners;
    public final LongAdder dependencies = new LongAdder();
    public final Lock readLock;
    public final Lock writeLock;

    public LockFreeNode(CommandEnvelope commandEnvelope, COS cos) {
        assert cos != null : "Não pode criar um node sem COS!";

        this.listeners = new Edge[cos.cosManager.graphs.length];
        fillWith(listeners, Edge::new);
        this.cos = cos;
        this.commandEnvelope = commandEnvelope;
        this.status = new AtomicInteger(NEW);
        var lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }

    public boolean testReady() {
        if (dependencies.intValue() == 0 && status.compareAndSet(INSERTED, READY)) {
            cos.releaseReady();
            return true;
        }
        return false;
    }


    @Override
    public String toString() {
        return "{status=" + status +
                ", data=" + commandEnvelope +
                ", dependencies=" + dependencies +
                ", listeners=" + listeners +
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
            Edge aux = this;
            while (aux.node != newNode && (aux.nextEdge.get() != null || !aux.nextEdge.compareAndSet(null, new Edge(newNode)))) {
                aux = aux.nextEdge.get();
            }
        }
    }
}

