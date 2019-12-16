package demo.hibrid.server.graph;


import demo.hibrid.server.ServerCommand;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class LockFreeNode<T> {

    public final T data;
    final COS<T> cos;

    final VertexType vertexType;
    LockFreeNode<T> next;
    LockFreeNode<T> prev;
    private boolean inserted = false;


    final AtomicBoolean reservedAtomic = new AtomicBoolean(false);
    final AtomicBoolean readyAtomic = new AtomicBoolean(false);
    private final AtomicBoolean removedAtomic = new AtomicBoolean(false);
    private final AtomicInteger dependenciesAtomic = new AtomicInteger(0);

    private final Edge<LockFreeNode<T>> dependentsHead = new Edge<>(null, VertexType.HEAD);
    private final Edge<LockFreeNode<T>> dependentsTail = new Edge<>(null, VertexType.TAIL);

    LockFreeNode(T data, VertexType vertexType, COS cos) {
        this.data = data;
        this.vertexType = vertexType;
        this.dependentsHead.next = this.dependentsTail;
        this.dependentsTail.prev = this.dependentsHead;
        this.cos = cos;
    }

    synchronized void insertBehind(LockFreeNode<T> node) {
        assert this.vertexType == VertexType.TAIL : "Tentativa de inserção de um nó fora do fim da fila.";

        node.next = this;
        this.prev.next = node;
        node.prev = this.prev;
        this.prev = node;
        node.inserted = true;
    }

    void goAway() {
        synchronized (this.prev) {
            synchronized (this) {
                synchronized (this.next) {              // esse nó pode ser o tail
                    this.prev.next = this.next;
                    this.next.prev = this.prev;
                }
            }
        }
    }

    void testReady() {
        if (inserted && dependenciesAtomic.get() == 0 && readyAtomic.compareAndSet(false, true)) {
            this.cos.release();
        }
    }

    void insertDependentNode(LockFreeNode<T> newNode) {
        synchronized (this.removedAtomic) {
            if (!this.removedAtomic.get()) {
                newNode.dependenciesAtomic.incrementAndGet();
                dependentsTail.insertBehind(newNode);
            }
        }
    }

    void markRemoved() {
        synchronized (this.removedAtomic) {
            if (this.removedAtomic.compareAndSet(false, true)) {
                var currentEdge = dependentsHead.next;
                while (currentEdge.vertexType != VertexType.TAIL) {
                    var dependentNode = currentEdge.node;
                    dependentNode.dependenciesAtomic.decrementAndGet();
                    dependentNode.testReady();
                    currentEdge = currentEdge.next;
                }
            }
        }
    }

    @Override
    public String toString() {
        switch (vertexType) {
            case HEAD:
                return "\n{HEAD}, " + next;
            case TAIL:
                return "\n{TAIL}";
            default:
                return "\nLockFreeNode{" +
                        " inserted=" + inserted +
                        ", reserved=" + reservedAtomic +
                        ", removed=" + removedAtomic +
                        ", ready=" + readyAtomic +
                        ", dependencies=" + dependenciesAtomic +
                        ", dependents=" + dependentsHead +
                        ", data=" + data +
                        "}, " + next;
        }
    }

    class Edge<N extends LockFreeNode<T>> {
        final VertexType vertexType;
        public final N node;
        private Edge<N> next;
        private Edge<N> prev;

        Edge(N node, VertexType v) {
            this(node, v, null, null);
        }
        Edge(N node, VertexType v, Edge<N> prev, Edge<N> next) {
            this.node = node;
            this.vertexType = v;
            this.prev = prev;
            this.next = next;
        }

        synchronized void insertBehind(N newNode) {
            assert this.vertexType == VertexType.TAIL : "Tentativa de inserção de um Edge fora do fim da fila.";

            var edge = new Edge<>(newNode, VertexType.MESSAGE, this.prev, this);
            this.prev.next = edge;
            this.prev = edge;
        }

        @Override
        public String toString() {
            switch (vertexType) {
                case HEAD:
                    return next.toString();
                case TAIL:
                    return "";
                default:
                    return ", " + ((ServerCommand) node.data).getCommandId() + next;
            }
        }
    }
}

