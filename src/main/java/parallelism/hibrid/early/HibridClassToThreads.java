package parallelism.hibrid.early;


import bftsmart.tom.core.messages.TOMMessage;

import java.util.Queue;

/**
 * @author eduardo
 */
public class HibridClassToThreads {
    public static int CONC = 0; // concurrent
    public static int SYNC = 1; //synchronized
    public int type;
    public int[] tIds;
    public int classId;

    public Queue<TOMMessage>[] queues;
    public int threadIndex = 0;

    public HibridClassToThreads(int classId, int type, int[] ids) {
        this.classId = classId;
        this.type = type;
        this.tIds = ids;
    }

    public void setQueues(Queue<TOMMessage>[] q) {
        if (q.length != tIds.length) {
            System.err.println("INCORRECT MAPPING");
        }
        this.queues = q;
    }

    public String toString() {
        String t = "CONC";
        if (type == SYNC) {
            t = "SYNC";
        }
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < tIds.length; j++) {
            sb.append(tIds[j] + ",");
        }
        return "CtoT [type:" + t + ",classID:" + classId + ", threads:" + sb.toString() + "]";
    }


}
