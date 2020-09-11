package demo.hibrid.server.scheduler;

import demo.hibrid.server.CommandEnvelope;
import demo.hibrid.server.graph.COS;
import demo.hibrid.server.graph.COS.Node;
import demo.hibrid.server.graph.COSManager;
import demo.hibrid.server.graph.LockFreeNode;
import demo.hibrid.server.queue.QueuesManager;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

import static demo.hibrid.server.graph.LockFreeNode.*;

public class LateScheduler extends Thread {

    private final int id;
    private final BlockingQueue<CommandEnvelope> queue;
    private final COS cos;
    private final COSManager cosManager;


    public LateScheduler(int id, QueuesManager queuesManager, COSManager cosManager) {
        super("LateScheduler[" + id + "]");
        this.id = id;
        this.queue = queuesManager.queues[id];
        this.cos = cosManager.graphs[id];
        this.cosManager = cosManager;

        assert cos.id == this.id : "Deram o COS["+cos.id+"] para o lateScheduler["+id+"]";
    }

    @Override
    public void run() {
        try {
            while (true) {
                var commands = new ArrayList<CommandEnvelope>();
                queue.drainTo(commands);
                for (var command : commands) {
                    schedule(command);
                }
                cleanRemovedNodes();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(id);
        }
    }

    public void schedule(CommandEnvelope commandEnvelope) {
        if (cos.createNodeFor(commandEnvelope)) {
            cleanRemovedNodesInsertDependenciesAndInsertNewNode(commandEnvelope, cos.head);
            cleanRemovedNodesAndInsertDependencies(commandEnvelope, cos.relatedNodes);
        } else {
            cleanRemovedNodesInsertDependenciesAndInsertNewNode(commandEnvelope, cos.relatedNodes);
            cleanRemovedNodesAndInsertDependencies(commandEnvelope, cos.head);
        }

        if (commandEnvelope.atomicCounter.decrementAndGet() <= 0){
            if (commandEnvelope.atomicNode.get().status.compareAndSet(NEW, INSERTED)){
                commandEnvelope.atomicNode.get().testReady();
            } else {
                throw new IllegalStateException("AtomicCounter == 0 and status != NEW");
            }
        }
    }

    private void cleanRemovedNodes() {
        var lastNode = cos.head;
        var currentNode = cos.head;
        while (currentNode.nextNode.get() != null) {
            currentNode = currentNode.nextNode.get();
            if (currentNode.value.status.get() == REMOVED) {
                lastNode.nextNode.compareAndSet(currentNode, currentNode.nextNode.get());
                for (var listenersHead : currentNode.value.listeners) {
                    listenersHead.forEach(it -> {
                        it.dependencies.decrement();
                        if (it.testReady()) {
                            cosManager.addToReadyQueue(it);
                        }
                    });
                }
                cos.cosManager.releaseSpace();
            }
            lastNode = currentNode;
        }
    }

    private void cleanRemovedNodesInsertDependenciesAndInsertNewNode(CommandEnvelope commandEnvelope, Node head) {
        Node newNode = new Node(commandEnvelope.atomicNode.get());
        var lastNode = head;
        var currentNode = head;
        while (!currentNode.nextNode.compareAndSet(null, newNode)){
            currentNode = currentNode.nextNode.get();
            while (currentNode.value.status.get() == REMOVED) { // remoção dos nodes
                lastNode.nextNode.compareAndSet(currentNode, currentNode.nextNode.get());
                currentNode = currentNode.nextNode.get();

                if (currentNode == null && lastNode.nextNode.compareAndSet(null, newNode)) { // acabou a lista, insere o novo nó
                    return;
                }
            }
            if (cos.conflictDefinition.isDependent(commandEnvelope, currentNode.value.commandEnvelope)) { // inserção de dependencias
                insertDependentNode(currentNode.value, commandEnvelope.atomicNode.get());
            }
            lastNode = currentNode;
        }
    }

    public void insertDependentNode(LockFreeNode oldNode, LockFreeNode newNode) {
        try {
            oldNode.readLock.lock();
            if (oldNode.status.get() != REMOVED) {
                oldNode.listeners[id].insert(newNode);
                newNode.dependencies.increment();
            }
        } finally {
            oldNode.readLock.unlock();
        }
    }



    private void cleanRemovedNodesAndInsertDependencies(CommandEnvelope commandEnvelope, Node head) {
        var lastNode = head;
        var currentNode = head;
        while (currentNode.nextNode.get() != null) {
            currentNode = currentNode.nextNode.get();
            while (currentNode.value.status.get() == REMOVED) { // remoção dos nodes
                lastNode.nextNode.compareAndSet(currentNode, currentNode.nextNode.get());
                if (currentNode.nextNode.get() == null) {
                    return;
                } else {
                    currentNode = currentNode.nextNode.get();
                }
            }
            if (cos.conflictDefinition.isDependent(commandEnvelope, currentNode.value.commandEnvelope)) { // inserção de dependencias
                insertDependentNode(currentNode.value, commandEnvelope.atomicNode.get());
            }
            lastNode = currentNode;
        }
    }


    @Override
    public String toString() {
        return "LateScheduler{" +
                "id=" + id +
                ", queue=" + queue +
                ", cos=" + cos.id +
                '}';
    }
}
