/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.server.graph;


import demo.hibrid.server.ServerCommand;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

/**
 * @author aldenio
 */
public class HibridCOS<T extends ServerCommand> { // TODO não precisava extender server command, está aqui só para debug.

    private final Semaphore space;                // counting semaphore for size of graph
    private final ConflictDefinition<T> conflictDefinition;
    private final Queue<HibridCOSNode<T>> nodes = new ConcurrentLinkedQueue<>();
    private final Semaphore globalReady;
    private final Semaphore ready = new Semaphore(0);  // tells if there is ready to execute

     HibridCOS(int limit, ConflictDefinition<T> cd, Semaphore globalReady) {
        this.space = new Semaphore(limit);
        this.conflictDefinition = cd;
        this.globalReady = globalReady;
    }

    HibridCOSNode<T> createNode(T serverCommand) {
        return new HibridCOSNode<T>(serverCommand, this);
    }

    public int size(){
         return nodes.size();
    }

    void insert(HibridCOSNode<T> node) throws InterruptedException {
        this.space.acquire();
        nodes.removeIf(HibridCOSNode::isDone);
        nodes.add(node);
    }

    void addDependencies(HibridCOSNode<T> newNode) {
        for (var oldNode : nodes) {
            if (isDependent(newNode.data, oldNode.data)) {
                newNode.dependsOn(oldNode);
            }
        }
    }

    public void remove(HibridCOSNode<T> node)  {
        node.markAsRemoved();
        this.space.release(1);
        node.releaseDependentNodes();
    }

    HibridCOSNode<T> tryGet()  {
        if (this.ready.tryAcquire()) {
            return getNextAvailableNode();
        }
        return null;
    }

    HibridCOSNode<T> get() throws InterruptedException {
        this.ready.acquire();
        return getNextAvailableNode();
    }

    private HibridCOSNode<T> getNextAvailableNode() {
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

    @Override
    public String toString() {
        return "HibridCOS{" +
                "space=" + space.availablePermits() +
                ", ready=" + ready.availablePermits() +
                ", nodes=" + nodes +
                '}';
    }

    public void release(int i) {
         this.ready.release(i);
         this.globalReady.release(i);
    }
}
