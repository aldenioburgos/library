/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.server.graph;


import java.util.concurrent.Semaphore;

/**
 * @author eduardo
 */
public abstract class COS<T> {

    private Semaphore space;                // counting semaphore for size of graph
    private Semaphore ready = new Semaphore(0);  // tells if there is ready to execute

    protected ConflictDefinition<T> conflictDefinition;


    public COS(int limit, ConflictDefinition<T> cd) {
        this.space = new Semaphore(limit);
        this.conflictDefinition = cd;
    }

    protected boolean isDependent(T thisRequest, T otherRequest) {
        return this.conflictDefinition.isDependent(thisRequest, otherRequest);
    }

    public void insert(T request) throws InterruptedException {
        space.acquire();
        int readyNum = COSInsert(request);
        this.ready.release(readyNum);
    }

    public void inserDependencies(T request) {
    }

    public void remove(T requestNode) throws InterruptedException {
        int readyNum = COSRemove(requestNode);
        this.space.release();
        this.ready.release(readyNum);
    }

    public T get() throws InterruptedException {
        this.ready.acquire();
        return COSGet();
    }

    protected abstract int COSInsert(T request) throws InterruptedException;

    protected abstract T COSGet() throws InterruptedException;

    protected abstract int COSRemove(T request) throws InterruptedException;


}