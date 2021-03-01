package parallelism.hibrid.late;

import parallelism.late.graph.Vertex;

/**
 * @author eduardo
 */
public class vNode {
    private Vertex vertex;                          // kind of Vertex
    private Object data;                               // the item kept in the graph node
    vNode next;                                     // next in the linked list

    eNode[] head;
    eNode[] tail;

    public vNode(Object data, Vertex vertex, int partitions) {
        this.data = data;                           // DATA and kind kept
        this.vertex = vertex;                       //

        head = new eNode[partitions];
        tail = new eNode[partitions];

        for (int i = 0; i < partitions; i++) {
            head[i] = new eNode(null, Vertex.HEAD);        // empty list of nodes that
            tail[i] = new eNode(null, Vertex.TAIL);        // DEPEND FROM THIS NODE
            head[i].setNext(tail[i]);
        }
        this.next = null;                           // LINKING

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


    // adds one more node that depends of this one
    public void insert(vNode newNode, int partition) {
        eNode neweNode = new eNode(newNode, Vertex.MESSAGE);
        eNode aux = head[partition];
        while (aux.getNext().getVertex() != Vertex.TAIL) {
            aux = aux.getNext();
        }
        neweNode.setNext(tail[partition]);
        aux.setNext(neweNode);
    }

}
