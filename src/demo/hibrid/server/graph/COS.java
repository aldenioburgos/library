package demo.hibrid.server.graph;

import demo.hibrid.server.CommandEnvelope;

import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static demo.hibrid.server.graph.LockFreeNode.*;

/**
 * @author aldenio & eduardo
 */
public class COS {

    public final int id;
    private final Node head;
    private final Node relatedNodes;
    public final Semaphore ready;

    private final ConflictDefinition<CommandEnvelope> conflictDefinition;
    public final COSManager cosManager;

    public COS(int id, ConflictDefinition<CommandEnvelope> conflictDefinition, COSManager cosManager) {
        this.id = id;
        this.head = new Node(null);
        this.relatedNodes = new Node(null);
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

    private LockFreeNode reserveNode() {
        var node = head.next.get();
        while (node != null && !node.value.status.compareAndSet(READY, RESERVED)) {
            node = node.next.get();
        }
        return (node != null)? node.value: null;
    }

    void releaseReady() {
        this.ready.release();
        cosManager.releaseReady();
    }


    public void cleanRemovedNodesInsertDependenciesAndInsertNewNode(CommandEnvelope commandEnvelope) {
        cosManager.acquireSpace();
        cleanRemovedNodesInsertDependenciesAndInsertNewNode(commandEnvelope, head);
        cleanRemovedNodesAndInsertDependencies(commandEnvelope, relatedNodes);
    }

    public void excludeRemovedNodesInsertDependencies(CommandEnvelope commandEnvelope) {
        cleanRemovedNodesInsertDependenciesAndInsertNewNode(commandEnvelope, relatedNodes);
        cleanRemovedNodesAndInsertDependencies(commandEnvelope, head);
    }


    private void cleanRemovedNodesInsertDependenciesAndInsertNewNode(CommandEnvelope commandEnvelope, Node head) {
        var newNode = new Node(commandEnvelope.getNode());
        var lastNode = head;
        var currentNode = head;
        while (!currentNode.next.compareAndSet(null, newNode)) {
            currentNode = currentNode.next.get();

            while (currentNode.value.status.get() == REMOVED) { // remoção dos nodes
                lastNode.next.compareAndSet(currentNode, currentNode.next.get());
                if (currentNode.next.compareAndSet(null,  newNode)) {
                    return;
                } else {
                    currentNode = currentNode.next.get();
                }
            }

            if (conflictDefinition.isDependent(commandEnvelope, currentNode.value.data)) { // inserção de dependencias
                currentNode.value.insertDependentNode(commandEnvelope.getNode());
            }
            lastNode = currentNode;
        }
    }


    private void cleanRemovedNodesAndInsertDependencies(CommandEnvelope commandEnvelope, Node head) {
        var lastNode = head;
        var currentNode = head;
        while (currentNode.next.get() != null) {
            currentNode = currentNode.next.get();
            while (currentNode.value.status.get() == REMOVED) { // remoção dos nodes
                lastNode.next.compareAndSet(currentNode, currentNode.next.get());
                if (currentNode.next.get() == null) {
                    return;
                } else {
                    currentNode = currentNode.next.get();
                }
            }
            if (conflictDefinition.isDependent(commandEnvelope, currentNode.value.data)) { // inserção de dependencias
                currentNode.value.insertDependentNode(commandEnvelope.getNode());
            }
            lastNode = currentNode;
        }
    }

    @Override
    public String toString() {
        return "\n\tCOS-" + id + "{" +
                "ready=" + this.ready.availablePermits() +
                ",\n\t\tnodes=" + this.head +
                ",\n\t\trelatedNodes=" + this.relatedNodes +
                "\n\t}";
    }
}



class Node {
    public final LockFreeNode value;
    public final AtomicReference<Node> next = new AtomicReference<>();

    public Node(LockFreeNode value) {
        this.value = value;
    }


    @Override
    public String toString() {
        if (value == null) return "[" + ((next.get() == null) ? "]" : next.get());
        else return "\n\t\t\t"+value.toString() + ((next.get() == null) ? "\n\t\t]" : ", " + next.get());

    }

}
