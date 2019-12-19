package demo.hibrid.server.graph;

import java.util.Optional;
import java.util.concurrent.Semaphore;

/**
 * @author aldenio & eduardo
 */
public class COS<T> {

    private final LockFreeNode<T> head = new LockFreeNode<>(null, VertexType.HEAD, this);
    private final LockFreeNode<T> tail = new LockFreeNode<>(null, VertexType.TAIL, this);
    private final ConflictDefinition<T> conflictDefinition;
    private final COSManager cosManager;
    private final Semaphore ready;

    public COS(ConflictDefinition<T> conflictDefinition, COSManager cosManager) {
        this.conflictDefinition = conflictDefinition;
        this.ready = new Semaphore(0);
        this.cosManager = cosManager;
        this.head.next = tail;
        this.tail.prev = head;
    }

    public LockFreeNode<T> createNode(T serverCommand) {
        return new LockFreeNode<T>(serverCommand, VertexType.MESSAGE, this);
    }

    public Optional<T> tryGet() {
        if (this.ready.tryAcquire()) {
            var data = COSGet().data;
            return Optional.of(data);
        }
        return Optional.empty();
    }

    private LockFreeNode<T> COSGet() {
        LockFreeNode<T> aux = head;
        while (true) {
            aux = aux.next;
            if (aux.readyAtomic.get() && aux.reservedAtomic.compareAndSet(false, true)) {
                break;
            }
            if (aux.vertexType == VertexType.TAIL) {
                aux = head;
            }
        }
        return aux;
    }

    void release() {
        this.ready.release();
        this.cosManager.release();
    }


    public void insert(LockFreeNode<T> newNode) {
        cleanRemovedNodes();
        tail.insertBehind(newNode);
        newNode.testReady();
    }

    private void cleanRemovedNodes() {
        LockFreeNode<T> node = head.next;
        while (node.vertexType != VertexType.TAIL) {
            if (node.isRemoved()) {
                node.goAway();
            }
            node = node.next;
        }
    }

    public void insertDependencies(LockFreeNode<T> newNode) {
        LockFreeNode<T> node = head.next;
        while (node.vertexType != VertexType.TAIL) {
            if (this.conflictDefinition.isDependent(newNode.data, node.data)) {
                node.insertDependentNode(newNode);
            }
            node = node.next;
        }
    }

    @Override
    public String toString() {
        return "COS{" +
                "ready=" + this.ready.availablePermits() +
                ", nodes=" + this.head.toString() +
                "}";
    }

}