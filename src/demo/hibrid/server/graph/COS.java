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


    public boolean createNodeFor(CommandEnvelope commandEnvelope) {
        return (commandEnvelope.atomicNode.get() == null &&
                commandEnvelope.atomicNode.compareAndSet(null, new LockFreeNode(commandEnvelope, this)));
    }

    public Optional<CommandEnvelope> tryGet() {
        if (this.ready.tryAcquire()) {
            CommandEnvelope data = reserveNode().commandEnvelope;
            return Optional.of(data);
        }
        return Optional.empty();
    }

    private LockFreeNode reserveNode() {
        var node = head.atomicNext.get();
        while (node != null && !node.value.status.compareAndSet(READY, RESERVED)) {
            node = node.atomicNext.get();
        }
        return (node != null) ? node.value : null;
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
        Node newNode = new Node(commandEnvelope.atomicNode.get());
        var lastNode = head;
        var currentNode = head;
        while (!currentNode.atomicNext.compareAndSet(null, newNode)){
            currentNode = currentNode.atomicNext.get();
            while (currentNode.value.status.get() == REMOVED) { // remoção dos nodes
                lastNode.atomicNext.compareAndSet(currentNode, currentNode.atomicNext.get());
                currentNode = currentNode.atomicNext.get();

                if (currentNode == null && lastNode.atomicNext.compareAndSet(null, newNode)) { // acabou a lista, insere o novo nó
                    return;
                }
            }
            if (conflictDefinition.isDependent(commandEnvelope, currentNode.value.commandEnvelope)) { // inserção de dependencias
                currentNode.value.insertDependentNode(commandEnvelope.atomicNode.get());
            }
            lastNode = currentNode;
        }
    }

    private int size() {
        var counter = 0;
        var aux = head;
        while (aux.atomicNext.get() != null) {
            counter++;
            aux = aux.atomicNext.get();
        }
        return counter;

    }


    private void cleanRemovedNodesAndInsertDependencies(CommandEnvelope commandEnvelope, Node head) {
        var lastNode = head;
        var currentNode = head;
        while (currentNode.atomicNext.get() != null) {
            currentNode = currentNode.atomicNext.get();
            while (currentNode.value.status.get() == REMOVED) { // remoção dos nodes
                lastNode.atomicNext.compareAndSet(currentNode, currentNode.atomicNext.get());
                if (currentNode.atomicNext.get() == null) {
                    return;
                } else {
                    currentNode = currentNode.atomicNext.get();
                }
            }
            if (conflictDefinition.isDependent(commandEnvelope, currentNode.value.commandEnvelope)) { // inserção de dependencias
                currentNode.value.insertDependentNode(commandEnvelope.atomicNode.get());
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


    class Node {
        public final LockFreeNode value;
        public final AtomicReference<Node> atomicNext = new AtomicReference<>(null);

        public Node(LockFreeNode value) {
            this.value = value;
        }


        @Override
        public String toString() {
            if (value == null) return "[" + ((atomicNext.get() == null) ? "]" : atomicNext.get());
            else return "\n\t\t\t" + value.toString() + ((atomicNext.get() == null) ? "\n\t\t]" : ", " + atomicNext.get());

        }

    }

}

