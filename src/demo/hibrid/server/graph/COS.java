package demo.hibrid.server.graph;

import java.util.Optional;
import java.util.concurrent.Semaphore;

import static demo.hibrid.server.graph.LockFreeNode.READY;
import static demo.hibrid.server.graph.LockFreeNode.RESERVED;
import static demo.hibrid.server.graph.VertexType.HEAD;
import static demo.hibrid.server.graph.VertexType.NODE;

/**
 * @author aldenio & eduardo
 */
public class COS<T> {

    private final LockFreeNode<T> head = new LockFreeNode<>(HEAD);
    private final LockFreeNode<T> tail = head.next;
    private final ConflictDefinition<T> conflictDefinition;
    private final COSManager cosManager;
    private final Semaphore ready;

    public COS(ConflictDefinition<T> conflictDefinition, COSManager cosManager) {
        this.conflictDefinition = conflictDefinition;
        this.ready = new Semaphore(0);
        this.cosManager = cosManager;
    }

    public LockFreeNode<T> createNode(T serverCommand) {
        return new LockFreeNode<>(serverCommand, NODE, this);
    }

    public Optional<T> tryGet() {
        if (this.ready.tryAcquire()) {
            var data = reserveNode().data;
            return Optional.of(data);
        }
        return Optional.empty();
    }

    /**
     * Reserve a ready node and return it.
     */
    private LockFreeNode<T> reserveNode() {
        var node = head.next;
        while (node != tail && !node.status.compareAndSet(READY, RESERVED)) {
            node = node.next;
        }
        assert node != tail : "Não foi possível reservar um NODE!";
        return node;
    }

    void releaseReady() {
        this.ready.release();
        this.cosManager.releaseReady();
    }


    public void insert(LockFreeNode<T> newNode) {
        var lastNode = head;
        var currentNode = head.next;
        while (currentNode != tail) {

            while (currentNode.status.get() == LockFreeNode.REMOVED) { // remoção dos nodes
                lastNode.next = currentNode.next;
                currentNode = currentNode.next;
            }

            if (currentNode == tail) break; // se acabou a lista, encerra

            if (conflictDefinition.isDependent(newNode.data, currentNode.data)) { // inserção de dependencias
                currentNode.insertDependentNode(newNode);
            }
            lastNode = currentNode;
            currentNode = currentNode.next;
        }

        lastNode.insert(newNode);
    }


    public void insertDependencies(LockFreeNode<T> newNode) {
        LockFreeNode<T> node = head.next;
        while (node != tail) {
            if (conflictDefinition.isDependent(newNode.data, node.data)) {
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
