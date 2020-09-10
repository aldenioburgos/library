package demo.hibrid.server.graph;


import demo.hibrid.server.CommandEnvelope;
import jdk.internal.vm.annotation.Contended;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class LockFreeNode {
    @Contended
    public final AtomicBoolean inserted = new AtomicBoolean(false);
    @Contended
    public final AtomicBoolean ready = new AtomicBoolean(false);
    @Contended
    public final AtomicBoolean reserved = new AtomicBoolean(false);
    @Contended
    public final AtomicBoolean removed = new AtomicBoolean(false);

    public final CommandEnvelope commandEnvelope;
    public final COS cos;

    private final Edge listeners;
    private final LongAdder dependencies = new LongAdder();
    private final Lock readLock;
    private final Lock writeLock;

    public LockFreeNode(CommandEnvelope commandEnvelope, COS cos) {
        assert cos != null : "Não pode criar um node sem COS!";
        this.listeners = new Edge(null);
        this.cos = cos;
        this.commandEnvelope = commandEnvelope;
        var lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }

    public void insertDependentNode(LockFreeNode newNode) {
        try {
            readLock.lock();
            if (removed.get()) {
                var aux = listeners;
                while (aux.atomicNext.get() != null || !aux.atomicNext.compareAndSet(null, new Edge(newNode))) {
                    aux = aux.atomicNext.get();
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
            if (reserved.get() && removed.compareAndSet(false, true)) {
                var aux = this.listeners.atomicNext.get();
                while (aux != null) {
                    aux.node.dependencies.decrement();
                    aux.node.testReady();
                    aux = aux.atomicNext.get();
                }
            }
        } finally {
            writeLock.unlock();
        }
    }


    public void testReady() {
        if (dependencies.intValue() == 0 && inserted.get() && ready.compareAndSet(false, true)) {
            cos.releaseReady();
        }
    }


    @Override
    public String toString() {
        var status = "" + (inserted.get() ? 1 : 0) + (ready.get() ? 1 : 0) + (reserved.get() ? 1 : 0) + (removed.get() ? 1 : 0);
        return "{status=" + status +
                ", data=" + commandEnvelope +
                ", dependencies=" + dependencies +
                ", listeners=" + listeners +
                '}';
    }

    class Edge {
        public final LockFreeNode node;
        public final AtomicReference<Edge> atomicNext = new AtomicReference<>();

        public Edge(LockFreeNode node) {
            this.node = node;
        }

        @Override
        public String toString() {
            return ((node == null) ? "[" : node.commandEnvelope.command.id) + ((atomicNext.get() == null) ? "]" : ", " + atomicNext);
        }
    }
}

