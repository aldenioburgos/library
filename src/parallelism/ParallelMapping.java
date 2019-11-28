/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parallelism;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;


/**
 *
 * @author alchieri
 */
public class ParallelMapping {

    public static int CONC_ALL = -1;
    public static int SYNC_ALL = -2;
    public static int CONFLICT_RECONFIGURATION = -3;

    private ClassToThreads[] classes;
    private BlockingQueue[] queues;
    private CyclicBarrier reconfBarrier;

    
    
    public ParallelMapping(int numberOfThreads, ClassToThreads[] cToT) {
        this.reconfBarrier = new CyclicBarrier(numberOfThreads + 1);

        // cria as filas
        this.queues = new BlockingQueue[numberOfThreads];
        for (int i = 0; i < queues.length; i++) {
            queues[i] = new FIFOQueue();
        }

        this.classes = cToT;
        for (ClassToThreads aClass : this.classes) {
            BlockingQueue[] blockingQueues = new BlockingQueue[aClass.tIds.length];
            for (int i = 0; i < blockingQueues.length; i++) {
                blockingQueues[i] = queues[aClass.tIds[i]];
            }
            aClass.setQueues(blockingQueues);
        }
    }
    
    public int getNumWorkers(){
        return this.queues.length;
    }
    
    public ClassToThreads getClass(int id){
        for (ClassToThreads aClass : this.classes) {
            if (aClass.classId == id) {
                return aClass;
            }
        }
        return null;
    }

    public CyclicBarrier getBarrier(int classId) {
        return this.getClass(classId).barrier;
    }

    public int getExecutorThread(int classId) {
        return this.getClass(classId).tIds[0];
    }

    public CyclicBarrier getReconfBarrier() {
        return reconfBarrier;
    }
    
    public BlockingQueue[] getAllQueues() {
        return queues;
    }

}
