/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parallelism.late.graph;

import parallelism.MessageContextPair;
import parallelism.late.CBASEScheduler;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author eduardo
 */

public abstract class DependencyGraph extends COS {

    protected vNode head;                                 // always exist
    protected vNode tail;                                 // in the list: lower and highest

    public DependencyGraph(int limit, CBASEScheduler scheduler) {
        super(limit, scheduler);
        head = new DependencyGraph.vNode(null, Vertex.HEAD);
        tail = new DependencyGraph.vNode(null, Vertex.TAIL);
        head.setNext(tail);
    }

    // ----------------------------------------------------------------------
    // --------- a vertex of the graph - graph kept as a linked list
    public class vNode {                                        // v_ertex Node

        private Vertex vertex;                          // kind of Vertex
        private Object data;                               // the item kept in the graph node
        private int depends;                            // number of nodes it depends of
        private vNode next;                                     // next in the linked list
        private boolean reserved;                       // if reserved for execution

        protected eNode head;
        protected eNode tail;

        final private ReentrantLock lock;

        public vNode(Object data, Vertex vertex) {
            this.data = data;                           // DATA and kind kept
            this.vertex = vertex;                       //
            this.next = null;                           // LINKING
            lock = new ReentrantLock();                 // LOCK
            depends = 0;                                // nodes THIS NODE DEPENDS OF
            head = new eNode(null, Vertex.HEAD);        // empty list of nodes that
            tail = new eNode(null, Vertex.TAIL);        // DEPEND ON THIS NODE
            head.setNext(tail);                         //
            reserved = false;                          // not reserved for execution -- used in CoarseGrained, FineGrained, ...
        }

        // DATA kept in node and kind of node (head, tail, message)
        public MessageContextPair getAsRequest() {
            return (MessageContextPair) data;
        }

        public Object getData() {
            return data;
        }


        public Vertex getVertex() {
            return vertex;
        }

        // LINKING info and setting
        public void setNext(vNode next) {
            this.next = next;
        }

        public vNode getNext() {
            return next;
        }

        // LOCK: returns the lock - but does not lock it
        public Lock getLock() {
            return lock;
        }

        // DEPENDENCIES: NODES THAT THIS NODE DEPENDS OF
        public void dependsMore() {
            depends++;
        }           // one more node

        public int getDepends() {
            return depends;
        }    // return number of nodes this depends

        public int dependsLess() {                          // one node this depends was removed
            return --depends;
        }

        // DEPENDENCIES: NODES THAT DEPEND FROM THIS ONE
        public eNode getDependents() {
            return head;
        }       // the list of dependent nodes

        // adds one more node that depends of this one
        public void insert(vNode newNode) {
            eNode neweNode = new eNode(newNode, Vertex.MESSAGE);
            eNode aux = head;
            while (aux.getNext().getVertex() != Vertex.TAIL) {
                aux = aux.getNext();
            }
            aux.setNext(neweNode);
            neweNode.setNext(tail);
        }

        //retuns the number of nodes free for execution
        public int destroyEdges(boolean useLocks) {
            if (useLocks) {
                return lockDestroyEdges();
            } else {
                return lockFreeDestroyEdges();
            }

        }

        // FINE GRAINED DESTROY EDGES
        // when this node is removed, all edges pointing to nodes that depends from this
        // one are removed too
        public int lockDestroyEdges() {
            int freeNodes = 0;
            eNode aux = head;
            while (aux.getVertex() != Vertex.TAIL) {
                if (aux.getVertex() != Vertex.HEAD) {
                    aux.getDependentVNode().getLock().lock(); //DUVIDA: Esta usando o mesmo lock usado no percurso-HOH, não poderia ser outro?
                    if (aux.getDependentVNode().dependsLess() == 0) {
                        freeNodes++;
                    }
                    aux.getDependentVNode().getLock().unlock();
                }
                aux = aux.getNext();
            }
            return freeNodes;
        }

        // COARSE LOCK DESTROY EDGES
        public int lockFreeDestroyEdges() {
            int freeNodes = 0;
            eNode aux = head;
            while (aux.getVertex() != Vertex.TAIL) {
                if ((aux.getVertex() != Vertex.HEAD) &&
                        (aux.getDependentVNode().dependsLess() == 0)) {
                    freeNodes++;
                }
                aux = aux.getNext();
            }
            return freeNodes;
        }


        // RESERVE to execute:  consult and set
        public boolean isreserved() {
            return reserved;
        }

        public void reserved() {
            reserved = true;
        }

        // --------- eNodes are references to nodes that depend on this vNode
        public class eNode {                                    // e_dge Node

            private Vertex vertex;                      // kind of Vertex
            vNode whoIAm;                               // ref to one vNode that depends of this vNode
            eNode next;

            public eNode(vNode whoYouAre, Vertex v) {
                whoIAm = whoYouAre;
                this.vertex = v;
                next = null;
            }

            // return type of node (head, tail, ...)
            public Vertex getVertex() {
                return vertex;
            }

            // refer to vNode this eNodes represents
            public vNode getDependentVNode() {
                return whoIAm;
            }

            public eNode getNext() {
                return next;
            }

            public void setNext(eNode next) {
                this.next = next;
            }
        }
        // --------- end of eNode class -------------------------------------

    }
    // --------- end of class vNode
    // ----------------------------------------


    public String print() {
        var list = new StringBuilder("Head ");
        vNode aux = head.getNext();
        while (aux.getVertex() != Vertex.TAIL) {
            list.append(" ");
            list.append(aux.getData());
            list.append("<");
            list.append(aux.getDepends());
            list.append(">");
            aux = aux.getNext();
        }
        list.append(" Tail");
        return list.toString();
    }
}
