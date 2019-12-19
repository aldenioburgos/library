package demo.hibrid.server.graph;


import demo.hibrid.server.ServerCommand;

import java.util.concurrent.atomic.AtomicBoolean;

public class LockFreeNode<T> {

    public final T data;
    private final COS<T> cos;

    final VertexType vertexType;
    LockFreeNode<T> next;
    LockFreeNode<T> prev;
    private boolean inserted = false;
    private int dependencies = 0;


    final AtomicBoolean reservedAtomic = new AtomicBoolean(false);
    final AtomicBoolean readyAtomic = new AtomicBoolean(false);
    private boolean removed = false;

    private final Edge<LockFreeNode<T>> dependentsHead = new Edge<>(null, VertexType.HEAD);
    private final Edge<LockFreeNode<T>> dependentsTail = new Edge<>(null, VertexType.TAIL);

    LockFreeNode(T data, VertexType vertexType, COS cos) {
        this.data = data;
        this.vertexType = vertexType;
        this.dependentsHead.next = this.dependentsTail;
        this.dependentsTail.prev = this.dependentsHead;
        this.cos = cos;
    }

    public boolean isRemoved() {
        return removed;
    }

    void insertBehind(LockFreeNode<T> node) {
        assert this.vertexType == VertexType.TAIL : "Tentativa de inserção de um nó fora do fim da fila.";
        node.next = this;
        this.prev.next = node;
        node.prev = this.prev;
        this.prev = node;
        node.inserted = true;
    }

    void goAway() {
        this.prev.next = this.next;
        this.next.prev = this.prev;
    }

    void testReady() {
        if (inserted && dependencies == 0 && readyAtomic.compareAndSet(false, true)) {
            this.cos.release();
        }
    }

    /**
     * O scheluler é que quem executa esse método, quando está inserindo um novo node no grafo.
     * Como os workers podem executar o mátodo markRemoved ao mesmo tempo desse método aqui,
     * é preciso sincronizar o acesso à lista de dependências.
     */
    synchronized void insertDependentNode(LockFreeNode<T> newNode) {
        if (!this.removed) {
            newNode.dependencies++;
            this.dependentsTail.insertBehind(newNode);
        }
    }

    /**
     * Os workers executam este método e podem concorrer com o scheduler no acesso a lista de dependências do node
     * portanto é preciso sincronizar o acesso à lista de dependências.
     */
    synchronized void markRemoved() {
        assert !this.removed: "Tentativa de remover o mesmo node mais de uma vez.";

        this.removed = true;
        var currentEdge = this.dependentsHead.next;
        while (currentEdge.vertexType != VertexType.TAIL) {
            var dependentNode = currentEdge.node;
            dependentNode.dependencies--;
            dependentNode.testReady();
            currentEdge = currentEdge.next;
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
                        ", removed=" + removed +
                        ", ready=" + readyAtomic +
                        ", dependencies=" + dependencies +
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

        void insertBehind(N newNode) {
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
                    return ", " + ((ServerCommand) node.data).commandId + next;
            }
        }
    }
}

