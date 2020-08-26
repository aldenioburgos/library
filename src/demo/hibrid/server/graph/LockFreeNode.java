package demo.hibrid.server.graph;


import demo.hibrid.server.CommandEnvelope;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import static demo.hibrid.server.graph.VertexType.HEAD;
import static demo.hibrid.server.graph.VertexType.TAIL;

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

    private final VertexType nodeType;
    public final CommandEnvelope data;
    public LockFreeNode next;
    public final COS cos;

    private final AtomicInteger dependencies = new AtomicInteger(0);
    private final Edge listeners = new Edge(HEAD);
    private final Edge tail = listeners.next;
    private boolean lateSchedulerIsWorkingHere = false;

    public LockFreeNode(VertexType nodeType) {
        this(null, nodeType, null);
        if (nodeType == HEAD) {
            this.next = new LockFreeNode(TAIL);
        }
    }

    public LockFreeNode(CommandEnvelope data, COS cos) {
        this(data, VertexType.NODE, cos);
        assert data != null : "Não pode criar um node sem dados!";
        assert cos != null : "Não pode criar um node sem COS!";
        data.setNode(this);
    }

    private LockFreeNode(CommandEnvelope data, VertexType nodeType,  COS cos) {
        assert nodeType != null: "Não pode criar um node sem o seu tipo.";
        this.nodeType = nodeType;
        this.data = data;
        this.cos = cos;
    }

    public void insert(LockFreeNode newNode) {
        newNode.next = this.next;
        this.next = newNode;
        if (!newNode.testReady()) {
            newNode.status.compareAndSet(NEW, INSERTED);
        }
    }


    public void insertDependentNode(LockFreeNode newNode) {
        lateSchedulerIsWorkingHere = true;
        if (status.get() != REMOVED) {
            listeners.insert(newNode);
            newNode.dependencies.incrementAndGet();
        }
        lateSchedulerIsWorkingHere = false;
    }

    public void markRemoved() {
        if (status.compareAndSet(RESERVED, REMOVED)) {
            while(lateSchedulerIsWorkingHere) {
                waitLateSchedulerFinish();
            }
            var listener = listeners.next;
            while (listener != tail) {
                listener.node.dependencies.decrementAndGet();
                listener.node.testReady();
                listener = listener.next;
            }
        }
    }

    private void waitLateSchedulerFinish() {
        try {
            this.wait(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean testReady() {
        if (dependencies.get() == 0 && (status.compareAndSet(NEW, READY) || status.compareAndSet(INSERTED, READY))) {
            cos.releaseReady();
            return true;
        } else {
            return false;
        }
    }


    @Override
    public String toString() {
        return switch (nodeType) {
            case HEAD -> "[" + next;
            case TAIL -> "]";
            default -> "{" +
                    " status=" + status +
                    ", dependencies=" + dependencies +
                    ", dependents=" + listeners +
                    ", data=" + data +
                    "}, " + next;
        };
    }


    class Edge {
        public final VertexType edgeType;
        public final LockFreeNode node;
        private Edge next;

        Edge(VertexType v) {
            this(null, v, null);
            if (v == HEAD) {
                this.next = new Edge(TAIL);
            }
        }

        Edge(LockFreeNode node, VertexType edgeType, Edge next) {
            this.node = node;
            this.edgeType = edgeType;
            this.next = next;
        }

        void insert(LockFreeNode newNode) {
            this.next = new Edge(newNode, VertexType.NODE, this.next);
        }

        @Override
        public String toString() {
            return switch (edgeType) {
                case HEAD -> "[" + next;
                case TAIL -> "]";
                default -> ", " + node.data.command.id + next;
            };
        }
    }
}

