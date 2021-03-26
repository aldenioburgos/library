package parallelism.hibrid.late;

import parallelism.HoldsClassIdInterface;
import parallelism.MessageContextPair;
import parallelism.late.ConflictDefinition;
import parallelism.late.graph.Vertex;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * @author eduardo
 */
public class ExtendedLockFreeGraph {

    private HibridLockFreeNode head;
    private HibridLockFreeNode tail;

    public Semaphore ready = new Semaphore(0);  // tells if there is ready to execute
    public Semaphore space = null;
    protected ConflictDefinition cd;

    public int myPartition;

    public List<HibridLockFreeNode> checkDep;

    public ExtendedLockFreeGraph(ConflictDefinition cd, int myPartition, int subGraphSize) {
        head = new HibridLockFreeNode(null, Vertex.HEAD, this, 0, 0);
        tail = new HibridLockFreeNode(null, Vertex.TAIL, this, 0, 0);
        head.setNext(tail);
        this.cd = cd;
        this.myPartition = myPartition;
        this.checkDep = new LinkedList<>();
        this.space = new Semaphore(subGraphSize);
    }

    public HibridLockFreeNode get() throws InterruptedException {
        this.ready.acquire();
        HibridLockFreeNode aux = (HibridLockFreeNode) head;
        boolean found = false;
        while (!found) {
            if (aux.getVertex() == Vertex.TAIL) {
                aux = (HibridLockFreeNode) head;
            }

            aux = (HibridLockFreeNode) aux.getNext();

            if (aux.readyAtomic.get()) { //was marked as ready
                found = aux.reservedAtomic.compareAndSet(false, true);  // atomically set to reserve for exec
            }

        }
        return aux;
    }

    public void remove(HibridLockFreeNode o) throws InterruptedException {
        o.removed = true;
        o.testDepMeReady(); //post em ready dos grafos com novos nós prontos para execução
        this.space.release();
    }

    public void insert(HibridLockFreeNode newvNode, boolean dependencyOnly, boolean conflic) {
        if (dependencyOnly) {
            insertDependencies(newvNode);
        } else {
            try {
                this.space.acquire();
                insertNodeAndDependencies(newvNode);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
        if (conflic) {
            if (newvNode.atomicCounter.decrementAndGet() == 0) {
                newvNode.inserted = true;
                newvNode.testReady();
            }
        } else {
            newvNode.inserted = true;
            newvNode.testReady();
        }

    }

    private void insertDependencies(HibridLockFreeNode newvNode) {
        HibridLockFreeNode aux = (HibridLockFreeNode) head;
        HibridLockFreeNode aux2 = (HibridLockFreeNode) aux.getNext();
        while (aux2.getVertex() != Vertex.TAIL) {
            //HELPED REMOVE
            while (aux2.isRemoved()) {            // aux2 was removed, have to help
                //Se quiser da pra remover aux2 das listas depOn de quem depende dele -- poderia melhorar o testReady
                aux.setNext(aux2.getNext());        // bypass it on the linked list
                aux2 = (HibridLockFreeNode) aux.getNext();               // proceed with aux2 to next node
            }
            // this helps removing several consecutive marked to remove
            // in the limit case, aux2 is tail
            if ((aux.getVertex() != Vertex.HEAD) && this.cd.isDependent(newvNode.getData(), aux.getData())) {//if node conflicts
                //newvNode.dependsMore();                    // new node depends on one more
                newvNode.insertDepOn(aux, myPartition);
                aux.insert(newvNode, myPartition);                       // add edge from older to newer

            }
            if (aux2.getVertex() != Vertex.TAIL) {
                aux2 = (HibridLockFreeNode) aux2.getNext();
                aux = (HibridLockFreeNode) aux.getNext();
            }
        }
        if ((aux.getVertex() != Vertex.HEAD) && this.cd.isDependent(newvNode.getData(), aux.getData())) { //if node conflicts
            newvNode.insertDepOn(aux, myPartition);
            aux.insert(newvNode, myPartition);
        }                                                  // added all needed edges TO new node

        Iterator<HibridLockFreeNode> it = this.checkDep.iterator();
        while (it.hasNext()) {
            HibridLockFreeNode next = it.next();
            if (next.isRemoved()) {
                it.remove();
            } else {
                if (((HoldsClassIdInterface)newvNode.getData()).getClassId() != ((HoldsClassIdInterface)next.getData()).getClassId() &&
                        this.cd.isDependent(newvNode.getData(), next.getData())) {//if node conflicts
                    newvNode.insertDepOn(next, myPartition);
                    next.insert(newvNode, myPartition);                       // add edge from older to newer
                }
            }
        }

        this.checkDep.add(newvNode);

    }

    private void insertNodeAndDependencies(HibridLockFreeNode newvNode) throws InterruptedException {
        HibridLockFreeNode aux = (HibridLockFreeNode) head;
        HibridLockFreeNode aux2 = (HibridLockFreeNode) aux.getNext();

        while (aux2.getVertex() != Vertex.TAIL) {
            //HELPED REMOVE
            while (aux2.isRemoved()) {            // aux2 was removed, have to help
                //Se quiser da pra remover aux2 das listas depOn de quem depende dele -- poderia melhorar o testReady
                aux.setNext(aux2.getNext());        // bypass it on the linked list
                aux2 = (HibridLockFreeNode) aux.getNext();               // proceed with aux2 to next node
            }                                       // this helps removing several consecutive marked to remove
            // in the limit case, aux2 is tail
            if ((aux.getVertex() != Vertex.HEAD) && this.cd.isDependent(newvNode.getData(), aux.getData())) {//if node conflicts
                //newvNode.dependsMore();                    // new node depends on one more
                newvNode.insertDepOn(aux, myPartition);
                aux.insert(newvNode, myPartition);                       // add edge from older to newer

            }
            if (aux2.getVertex() != Vertex.TAIL) {
                aux2 = (HibridLockFreeNode) aux2.getNext();
                aux = (HibridLockFreeNode) aux.getNext();
            }
        }
        if ((aux.getVertex() != Vertex.HEAD) && this.cd.isDependent(newvNode.getData(), aux.getData())) { //if node conflicts
            newvNode.insertDepOn(aux, myPartition);
            aux.insert(newvNode, myPartition);

        }                                                  // added all needed edges TO new node
        newvNode.setNext(tail);                            // at the end of the list
        aux.setNext(newvNode);                             // insert new node

        Iterator<HibridLockFreeNode> it = this.checkDep.iterator();
        while (it.hasNext()) {
            HibridLockFreeNode next = it.next();
            if (next.isRemoved()) {
                it.remove();
            } else {
                if (this.cd.isDependent(newvNode.getData(), next.getData())) {//if node conflicts
                    newvNode.insertDepOn(next, myPartition);
                    next.insert(newvNode, myPartition);                       // add edge from older to newer
                }
            }
        }
    }

}
