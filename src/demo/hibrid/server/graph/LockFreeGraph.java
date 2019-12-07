/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.server.graph;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author aldenio
 */
public class LockFreeGraph<T> extends COS<T> {

    private LockFreeNode head = new LockFreeNode();

    public LockFreeGraph(int limit, ConflictDefinition cd) {
        super(limit, cd);
    }

    @Override
    public int COSInsert(T request) {
        var newNode = new LockFreeNode(request);
        cleanRemovedNodes();
        addDependencies(newNode);
        head.add(newNode);
        return newNode.testReady();
    }

    void cleanRemovedNodes() {
        var one = head;
        var two = one.next;
        while (two != null) {
            var zero = one;
            one = two;
            two = two.next;
            if (one.removedAtomic.get()) {
                zero.next = two;
            }
        }
    }

    public void addDependencies(LockFreeNode newNode) {
        var otherNode = head.next;
        while (otherNode != null) {
            if (isDependent(newNode.data, otherNode.data)) {
                newNode.dependsOn(otherNode);
            }
            otherNode = otherNode.next;
        }
    }


    @Override
    public T COSGet() {
        var node = head.next;
        while (node != null) {
            if (node.readyAtomic.get() && node.reservedAtomic.compareAndSet(false, true)) {
                break;
            }
            node = node.next;
        }
        return node.data;
    }

    @Override
    public int COSRemove(T o) throws InterruptedException {
        LockFreeNode data = ((LockFreeNode) o);
        //DUVIDA: acredito que não precisa ser atomico!
        data.removedAtomic.compareAndSet(false, true);
        return data.testDepMeReady();
    }


    class LockFreeNode {
        public final T data;                               // the item kept in the graph LockFreeNode
        LockFreeNode next;                                     // next in the linked list

        Edge myDependencies = new Edge();
        Edge dependentNodes = new Edge();

        AtomicBoolean reservedAtomic = new AtomicBoolean(false);
        AtomicBoolean removedAtomic = new AtomicBoolean(false);
        AtomicBoolean readyAtomic = new AtomicBoolean(false);
        boolean inserted = false;

        LockFreeNode() {
            this.data = null;
        }

        LockFreeNode(T data) {
            this.data = data;
        }



        void dependsOn(LockFreeNode node) {
            this.myDependencies.add(node);
            node.dependentNodes.add(this);
        }

        int testDepMeReady() {
            int freeNodes = 0;
            Edge ni = myDependencies.next;
            while (ni != null) {
                freeNodes += ni.node.testReady();
                ni = ni.next;
            }
            return freeNodes;
        }

        int testReady() {
            if (!inserted) {
                return 0;
            }
            var aux = myDependencies.next;
            while (aux != null) {
                if (!aux.node.removedAtomic.get()) {//not removed
                    return 0;
                }
                aux = aux.next;
            }
            if (readyAtomic.compareAndSet(false, true)) { //it is necessary to return true only once
                return 1;
            }
            return 0;
        }

        void add(LockFreeNode newNode) {
            var aux = this;
            while (aux.next != null) {
                aux = aux.next;
            }
            aux.next = newNode;
        }

    }

    class Edge {
        final LockFreeNode node;
        Edge next;

        Edge() {
            this.node = null;
        }

        Edge(LockFreeNode whoYouAre) {
            this.node = whoYouAre;
        }

        void add(LockFreeNode node) {
            var aux = this;
            while (aux.next != null) {
                aux = aux.next;
            }
            aux.next = new Edge(node);
        }

    }

}
