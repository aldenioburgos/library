package demo.hibrid.server.graph;


import demo.hibrid.request.Command;
import demo.util.Utils.Action;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import static demo.util.Utils.fillWith;


public class LockFreeNode {

    public final int requestId;
    public final Command command;
    public final int[] distinctPartitions;
    public final AtomicInteger atomicCounter;

    public boolean inserted = false;
    public boolean completed = false;
    public final AtomicBoolean ready = new AtomicBoolean(false);


    public final Edge[] listeners;
    public final LongAdder dependencies = new LongAdder();
    public final WriteLock writeLock;
    public final ReadLock readLock;

    public LockFreeNode(int requestId, Command command, int numPartitions) {
        var lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
        this.listeners = new Edge[numPartitions];
        fillWith(listeners, Edge::new);
        this.requestId = requestId;
        this.command = command;
        this.distinctPartitions = command.distinctPartitions();
        this.atomicCounter = new AtomicInteger(distinctPartitions.length);
    }


    public boolean isReady() {
        return (inserted && dependencies.intValue() == 0 && ready.compareAndSet(false, true));
    }

    @Override
    public String toString() {
        return "{status=" +
                ", dependencies=" + dependencies +
                ", listeners=" + Arrays.toString(listeners) +
                '}';
    }


    /**
     *
     */
    public static class Edge {
        public final LockFreeNode node;
        public Edge nextEdge;

        public Edge() {
            node = null;
        }

        public Edge(LockFreeNode node) {
            this.node = node;
        }

        @Override
        public String toString() {
            return ((node == null) ? "[" : node.command.id) + ((nextEdge == null) ? "]" : ", " + nextEdge);
        }

        public void forEach(Action<LockFreeNode> action) {
            Edge aux = this.nextEdge;
            while (aux != null) {
                action.apply(aux.node);
                aux = aux.nextEdge;
            }
        }

        public void insert(LockFreeNode newNode) {
            Edge aux = this;
            while (aux.nextEdge != null) {
                if (aux.node == newNode) return;
                aux = aux.nextEdge;
            }
            aux.nextEdge = new Edge(newNode);
        }
    }
}

