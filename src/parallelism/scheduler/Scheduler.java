/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parallelism.scheduler;

import parallelism.MessageContextPair;
import parallelism.ParallelMapping;


/**
 * @author eduardo
 */
public interface Scheduler {

    void schedule(MessageContextPair request);

    ParallelMapping getMapping();

    void scheduleReplicaReconfiguration();

    int getNumWorkers();
}
