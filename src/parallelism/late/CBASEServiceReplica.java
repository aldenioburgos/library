/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parallelism.late;

import bftsmart.tom.server.Executable;
import bftsmart.tom.server.Recoverable;
import bftsmart.util.ThroughputStatistics;
import parallelism.MessageContextPair;
import parallelism.ParallelMapping;
import parallelism.ParallelServiceReplica;
import parallelism.late.graph.DependencyGraph;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * @author eduardo
 */
public class CBASEServiceReplica extends ParallelServiceReplica {

    private CyclicBarrier recBarrier = new CyclicBarrier(2);

    public CBASEServiceReplica(int id, Executable executor, Recoverable recoverer, int numWorkers, ConflictDefinition cf, COSType graphType) {
        super(id, executor, recoverer, new CBASEScheduler(cf, numWorkers, graphType));
    }

    public CyclicBarrier getReconfBarrier() {
        return recBarrier;
    }

    @Override
    protected void initWorkers(int numWorkers, int idReplica) {
        statistics = new ThroughputStatistics(idReplica, numWorkers, "results_" + idReplica + ".txt", "");
        for (int tId = 0; tId < numWorkers; tId++) {
            new CBASEServiceReplicaWorker((CBASEScheduler) this.scheduler, tId).start();
        }
    }

    private class CBASEServiceReplicaWorker extends Thread {
        private CBASEScheduler scheduler;
        private int thread_id;

        public CBASEServiceReplicaWorker(CBASEScheduler scheduler, int thread_id) {
            this.thread_id = thread_id;
            this.scheduler = scheduler;
            this.setName("CBASEServiceReplicaWorker["+thread_id+"]");
        }

        public void run() {
            System.out.println("CBASEServiceReplicaWorker id =" + thread_id + " is " + Thread.currentThread());
            while (true) {
                Object node = scheduler.get();
                MessageContextPair msg = ((DependencyGraph.vNode) node).getAsRequest();
                if (msg.classId == ParallelMapping.CONFLICT_RECONFIGURATION) {
                    runConfictReconfiguration();
                } else {
                    runOperation(msg, thread_id);
                }
                scheduler.remove(node);
            }
        }

        private void runConfictReconfiguration() {
            try {
                getReconfBarrier().await();
                //TODO: trecho de código que não faz nada!
                getReconfBarrier().await();
            } catch (InterruptedException | BrokenBarrierException ex) {
                ex.printStackTrace();
            }
        }

    }
}
