
package parallelism.scheduler;

import bftsmart.tom.core.messages.TOMMessage;
import parallelism.MessageContextPair;
import parallelism.ParallelMapping;


/**
 *
 * @author eduardo
 */
public interface Scheduler {
   void schedule(MessageContextPair request);
   void schedule(TOMMessage request);
   ParallelMapping getMapping();
   void scheduleReplicaReconfiguration();
   int getNumWorkers();
}
