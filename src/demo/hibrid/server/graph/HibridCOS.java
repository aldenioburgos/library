/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.server.graph;


import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * @author eduardo
 */
public class HibridCOS<T> {

    private final Semaphore space;                // counting semaphore for size of graph
    private final ConflictDefinition<T> conflictDefinition;
    private final List<COSNode<T>> nodes = new LinkedList<>();

    Semaphore ready = new Semaphore(0);  // tells if there is ready to execute

     HibridCOS(int limit, ConflictDefinition<T> cd) {
        this.space = new Semaphore(limit);
        this.conflictDefinition = cd;
    }

    COSNode<T> createNode(T serverCommand) {
        return new COSNode<T>(serverCommand, this);
    }

    void insert(COSNode<T> node) throws InterruptedException {
        this.space.acquire();
        nodes.removeIf(COSNode::isDone);
        nodes.add(node);
    }

    void addDependencies(COSNode<T> newNode) {
        for (var oldNode : nodes) {
            if (isDependent(newNode.data, oldNode.data)) {
                newNode.dependsOn(oldNode);
            }
        }
    }

    public void remove(COSNode<T> node) throws InterruptedException {
        node.markAsRemoved();
        this.space.release(1);
        node.releaseDependentNodes();
    }

    COSNode<T> tryGet()  {
        if (this.ready.tryAcquire()) {
            return getNextAvailableNode();
        }
        return null;
    }

    COSNode<T> get() throws InterruptedException {
        this.ready.acquire();
        return getNextAvailableNode();
    }

    private COSNode<T> getNextAvailableNode() {
        for (var node : nodes) {
            if (node.tryReserve()) {
                return node;
            }
        }
        throw new IllegalStateException("Semáforo informa que tem nó pronto, mas não foi possível encontrar um nó ready na lista.");
    }

    protected boolean isDependent(T thisRequest, T otherRequest) {
        return this.conflictDefinition.isDependent(thisRequest, otherRequest);
    }

}
