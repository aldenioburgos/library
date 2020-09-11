package demo.hibrid.server.graph;

import demo.hibrid.server.CommandEnvelope;
import demo.util.Utils;
import demo.util.Utils.Filter;

import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static demo.hibrid.server.graph.LockFreeNode.*;

/**
 * @author aldenio & eduardo
 */
public class COS {

    public final int id;
    public final Node head;
    public final Node relatedNodes;
    public final Semaphore ready;

    public final ConflictDefinition<CommandEnvelope> conflictDefinition;
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


    void releaseReady() {
        this.ready.release();
        cosManager.releaseReady();
    }


    @Override
    public String toString() {
        return "\n\tCOS-" + id + "{" +
                "ready=" + this.ready.availablePermits() +
                ",\n\t\tnodes=" + this.head +
                ",\n\t\trelatedNodes=" + this.relatedNodes +
                "\n\t}";
    }


    public static class Node {
        public final LockFreeNode value;
        public final AtomicReference<Node> nextNode = new AtomicReference<>(null);

        public Node(LockFreeNode value) {
            this.value = value;
        }

        public Optional<LockFreeNode> find(Filter<LockFreeNode> filter) {
            if (value != null) throw new UnsupportedOperationException("Operação find só pode ser executada no head da lista.");
            Node node = this.nextNode.get();
            while (node != null && !filter.apply(node.value)) {
                node = node.nextNode.get();
            }
            return (node == null) ? Optional.empty() : Optional.of(node.value);
        }



        @Override
        public String toString() {
            if (value == null) return "[" + ((nextNode.get() == null) ? "]" : nextNode.get());
            else return "\n\t\t\t" + value.toString() + ((nextNode.get() == null) ? "\n\t\t]" : ", " + nextNode.get());

        }

    }

}

