package demo.hibrid.server.graph;


import demo.hibrid.server.CommandEnvelope;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class LockFreeNode {
    /*
     NEW -> INSERTED -> READY -> RESERVED -> REMOVED
     */
    public static final int NEW = 0;
    public static final int INSERTED = 1;
    public static final int READY = 2;
    public static final int RESERVED = 3;
    public static final int REMOVED = 5;
    public final AtomicInteger status = new AtomicInteger(NEW);
    public final CommandEnvelope data;
    public final COS cos;
    private final AtomicInteger dependencies = new AtomicInteger(0);
    private final Edge listeners;
    private boolean busy = false;

    public LockFreeNode(CommandEnvelope data, COS cos) {
        assert cos != null : "Não pode criar um node sem COS!";
        this.listeners = new Edge(null);
        this.cos = cos;
        this.data = data;
        if (data != null) {
            data.setNode(this);
        }
    }

    public void insertDependentNode(LockFreeNode newNode) {
        busy = true;
        if (status.get() != REMOVED) {
            var aux = listeners;
            while (!aux.next.compareAndSet(null, new Edge(newNode))) {
                aux = aux.next.get();
                if (aux.node == newNode) {
                    busy = false;
                    return;
                }
            }
            newNode.dependencies.incrementAndGet();
        }
        busy = false;
    }

    public void markRemoved() {
        if (status.compareAndSet(RESERVED, REMOVED)) {
            while (busy) {
                synchronized (this) {
                    try {
                        wait(0, 10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            var aux = this.listeners.next.get();
            while (aux != null) {
                aux.node.dependencies.decrementAndGet();
                aux.node.testReady();
                aux = aux.next.get();
            }
        }
    }


    public void testReady() {
        if (dependencies.get() <= 0 && status.compareAndSet(INSERTED, READY)) {
            cos.releaseReady();
        }
    }




    @Override
    public String toString() {
        return "{status=" + status +
                ", data=" + data +
                ", dependencies=" + dependencies +
                ", listeners=" + listeners +
                ", busy=" + busy +
                '}';
    }

    class Edge {
        public final LockFreeNode node;
        public final AtomicReference<Edge> next = new AtomicReference<>();

        public Edge(LockFreeNode node) {
            this.node = node;
        }

        @Override
        public String toString() {
            return ((node == null) ? "[" : node.data.command.id) + ((next.get() == null) ? "]" : ", " + next);
        }
    }
}

