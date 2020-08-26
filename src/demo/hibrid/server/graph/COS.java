package demo.hibrid.server.graph;

import demo.hibrid.server.CommandEnvelope;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Semaphore;

import static demo.hibrid.server.graph.LockFreeNode.READY;
import static demo.hibrid.server.graph.LockFreeNode.RESERVED;
import static demo.hibrid.server.graph.VertexType.HEAD;

/**
 * @author aldenio & eduardo
 */
public class COS {

    private final LockFreeNode head = new LockFreeNode(HEAD);
    private final LockFreeNode tail = head.next;
    private final ConflictDefinition<CommandEnvelope> conflictDefinition;
    private final COSManager cosManager;
    private final Semaphore ready;

    public COS(ConflictDefinition<CommandEnvelope> conflictDefinition, COSManager cosManager) {
        this.conflictDefinition = conflictDefinition;
        this.ready = new Semaphore(0);
        this.cosManager = cosManager;
    }


    public void createNodeFor(CommandEnvelope commandEnvelope) {
        new LockFreeNode(commandEnvelope, this);
    }

    public Optional<CommandEnvelope> tryGet() {
        if (this.ready.tryAcquire()) {
            var data = reserveNode().data;
            return Optional.of(data);
        }
        return Optional.empty();
    }

    /**
     * Reserve a ready node and return it.
     */
    private LockFreeNode reserveNode() {
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


    public void excludeRemovedNodes_insertDependencies_insertNewNode(CommandEnvelope commandEnvelope) {
        var lastNode = head;
        var currentNode = head.next;
        while (currentNode != tail) {

            while (currentNode != tail && currentNode.status.get() == LockFreeNode.REMOVED) { // remoção dos nodes
                lastNode.next = currentNode.next;
                currentNode = currentNode.next;
            }

            if (currentNode == tail) break; // se acabou a lista, encerra

            if (conflictDefinition.isDependent(commandEnvelope, currentNode.data)) { // inserção de dependencias
                currentNode.insertDependentNode(commandEnvelope.getNode());
            }
            lastNode = currentNode;
            currentNode = currentNode.next;
        }

        lastNode.insert(commandEnvelope.getNode());
    }

    public void insertDependencies(CommandEnvelope commandEnvelope) {
        LockFreeNode node = head.next;
        while (node != tail) {
            if (conflictDefinition.isDependent(commandEnvelope, node.data)) {
                node.insertDependentNode(commandEnvelope.getNode());
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
