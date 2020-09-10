package demo.hibrid.server.graph;


import demo.hibrid.server.CommandEnvelope;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class LockFreeNode {
    public final PaddedAtomicBoolean inserted = new PaddedAtomicBoolean(false);
    public final PaddedAtomicBoolean ready = new PaddedAtomicBoolean(false);
    public final PaddedAtomicBoolean reserved = new PaddedAtomicBoolean(false);
    public final PaddedAtomicBoolean removed = new PaddedAtomicBoolean(false);

    public final CommandEnvelope commandEnvelope;
    public final COS cos;

    private final ConcurrentHashMap<Integer, Edge> listeners;
    private final LongAdder dependencies = new LongAdder();
    private final Lock readLock;
    private final Lock writeLock;

    public LockFreeNode(CommandEnvelope commandEnvelope, COS cos) {
        assert cos != null : "Não pode criar um node sem COS!";
        this.listeners = new ConcurrentHashMap<>();
        for (int i = 0; i < cos.cosManager.graphs.length; i++) {
            listeners.put(i, new Edge(null));
        }
        this.cos = cos;
        this.commandEnvelope = commandEnvelope;
        this.status = new AtomicInteger(NEW);
        var lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }

    public void insertDependentNode(LockFreeNode newNode) {
        try {
            readLock.lock();
            if (status.get() != REMOVED) {
                var aux = listeners.get(cos.id);
                while (aux.nextEdge.get() != null || !aux.nextEdge.compareAndSet(null, new Edge(newNode))) {
                    aux = aux.nextEdge.get();
                    if (aux.node == newNode) {
                        return;     // não insere o mesmo nó mais de uma vez!
                    }
                }
                newNode.dependencies.increment();
            }
        } finally {
            readLock.unlock();
        }

    }

    public void markRemoved() {
        try {
            writeLock.lock();
            if (status.compareAndSet(RESERVED, REMOVED)) {
                for (var listenerHead : listeners.values()) {
                    var aux = listenerHead.nextEdge.get();
                    while (aux != null) {
                        aux.node.dependencies.decrement();
                        aux.node.testReady();
                        aux = aux.nextEdge.get();
                    }
                }
            }
        } finally {
            writeLock.unlock();
        }
    }


    public void testReady() {
        if (dependencies.intValue() == 0 && status.compareAndSet(INSERTED, READY)) {
            cos.releaseReady();
        }
    }


    @Override
    public String toString() {
        return "{status=" + status +
                ", data=" + commandEnvelope +
                ", dependencies=" + dependencies +
                ", listeners=" + listeners +
                '}';
    }

    class Edge {
        public final LockFreeNode node;
        public final AtomicReference<Edge> nextEdge = new AtomicReference<>();

        public Edge(LockFreeNode node) {
            this.node = node;
        }

        @Override
        public String toString() {
            return ((node == null) ? "[" : node.commandEnvelope.command.id) + ((nextEdge.get() == null) ? "]" : ", " + nextEdge);
        }
    }
}

