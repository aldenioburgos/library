/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parallelism.late;

import parallelism.MessageContextPair;
import parallelism.ParallelMapping;
import parallelism.late.graph.COS;
import parallelism.late.graph.CoarseGrainedLock;
import parallelism.late.graph.FineGrainedLock;
import parallelism.late.graph.LockFreeGraph;
import parallelism.scheduler.Scheduler;

import java.util.Objects;

/**
 * @author eduardo
 */
public class CBASEScheduler implements Scheduler {

    private COS cos;
    private int numWorkers;

    private ConflictDefinition conflictDef;

    public CBASEScheduler(int numWorkers, COSType cosType) {
        this(null, numWorkers, cosType);
    }

    public CBASEScheduler(ConflictDefinition cd, int numWorkers, COSType cosType) {
        int limit = 150;
        if (cosType == null || cosType == COSType.coarseLockGraph) {
            this.cos = new CoarseGrainedLock(limit, this);
        } else if (cosType == COSType.fineLockGraph) {
            this.cos = new FineGrainedLock(limit, this);
        } else if (cosType == COSType.lockFreeGraph) {
            this.cos = new LockFreeGraph(limit, this);
        } else {
            this.cos = new CoarseGrainedLock(limit, this);
        }
        this.numWorkers = numWorkers;
        this.conflictDef = Objects.requireNonNullElseGet(cd, DefaultConflictDefinition::new);
    }


    public boolean isDependent(MessageContextPair thisRequest, MessageContextPair otherRequest) {
        if (thisRequest.classId == ParallelMapping.CONFLICT_RECONFIGURATION || otherRequest.classId == ParallelMapping.CONFLICT_RECONFIGURATION) {
            return true;
        }
        return this.conflictDef.isDependent(thisRequest, otherRequest);
    }


    @Override
    public int getNumWorkers() {
        return this.numWorkers;
    }


    @Override
    public void schedule(MessageContextPair request) {
        System.out.println("CBASEScheduler.schedule requested in " + Thread.currentThread());
        try {
            cos.insert(request);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public Object get() {
        System.out.println("CBASEScheduler.get requested in " + Thread.currentThread());
        try {
            return cos.get();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void remove(Object requestRequest) {
        System.out.println("CBASEScheduler.remove requested in " + Thread.currentThread());
        try {
            cos.remove(requestRequest);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void scheduleReplicaReconfiguration() {
        MessageContextPair m = new MessageContextPair(null, ParallelMapping.CONFLICT_RECONFIGURATION, -1, null);
        schedule(m);
    }

    @Override
    public ParallelMapping getMapping() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


}
