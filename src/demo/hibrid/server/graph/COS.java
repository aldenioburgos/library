/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.server.graph;

import demo.hibrid.server.ServerCommand;

import java.util.Optional;
import java.util.concurrent.Semaphore;

/**
 * @author aldenio & eduardo
 */
public class COS<T> {

    private final LockFreeNode<T> head = new LockFreeNode<>(null, VertexType.HEAD, this);
    private final LockFreeNode<T> tail = new LockFreeNode<>(null, VertexType.TAIL, this);
    private ConflictDefinition<T> conflictDefinition;
    private final COSManager cosManager;
    private Semaphore ready;

    public COS(ConflictDefinition<T> conflictDefinition, COSManager cosManager) {
        this.ready = new Semaphore(0);
        this.conflictDefinition = conflictDefinition;
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
            if (aux.vertexType == VertexType.TAIL) {
                aux = head;
            }
            aux = aux.next;
            if (aux.readyAtomic.get() && aux.reservedAtomic.compareAndSet(false, true)) {
                break;
            }
        }
        return aux;
    }

    void release() {
        this.ready.release();
        this.cosManager.release();
    }


    public void insert(LockFreeNode<T> newNode) {
        tail.insertBehind(newNode);
        newNode.testReady();
    }

    public void insertDependencies(LockFreeNode<T> newNode) {
        LockFreeNode<T> currentNode = head.next;
        while (currentNode.vertexType != VertexType.TAIL) {
            if (this.conflictDefinition.isDependent(newNode.data, currentNode.data)) {
                currentNode.insertDependentNode(newNode);
            }
            currentNode = currentNode.next;
        }
    }


    public void remove(LockFreeNode<T> node) {
        node.markRemoved();
        node.goAway();
    }

    @Override
    public String toString() {
        return "COS{" +
                "ready=" + this.ready.availablePermits() +
                ", nodes=" + this.head.toString() +
                "}";
    }

}