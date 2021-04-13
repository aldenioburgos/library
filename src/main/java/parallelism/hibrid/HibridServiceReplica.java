package parallelism.hibrid;

import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.Executable;
import bftsmart.tom.server.Recoverable;
import bftsmart.tom.server.SingleExecutable;
import bftsmart.util.MultiOperationRequest;
import bftsmart.util.ThroughputStatistics;
import parallelism.MessageContextPair;
import parallelism.MultiOperationCtx;
import parallelism.ParallelServiceReplica;
import parallelism.hibrid.early.EarlySchedulerMapping;
import parallelism.hibrid.early.HibridClassToThreads;
import parallelism.hibrid.early.HibridScheduler;
import parallelism.hibrid.early.TOMMessageWrapper;
import parallelism.hibrid.late.ExtendedLockFreeGraph;
import parallelism.hibrid.late.HibridLockFreeNode;
import parallelism.late.ConflictDefinition;
import parallelism.late.graph.Vertex;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Queue;

/**
 * @author eduardo
 */
public class HibridServiceReplica extends ParallelServiceReplica {

    private final ExtendedLockFreeGraph[] subgraphs;

    public HibridServiceReplica(int id, Executable executor, Recoverable recoverer, int numPartitions, ConflictDefinition cd, int lateWorkers) {
        super(id, executor, recoverer, numPartitions);
        System.out.println("Criou um hibrid scheduler: partitions (early) = " + numPartitions + " workers (late) = " + lateWorkers);

        //statistics = new ThroughputStatistics(id, lateWorkers, "resultsHybrid_" + id + "_" + numPartitions + "_" + lateWorkers + ".txt", "");
        subgraphs = createSubGraphs(numPartitions, cd);
        initLateWorkers(lateWorkers, id, numPartitions);
    }

    private ExtendedLockFreeGraph[] createSubGraphs(int numPartitions, ConflictDefinition cd) {
        var graphs = new ExtendedLockFreeGraph[numPartitions];
        for (int i = 0; i < numPartitions; i++) {
            graphs[i] = new ExtendedLockFreeGraph(cd, i, 150 / numPartitions);
        }
        return graphs;
    }

    @Override
    protected void createScheduler(int initialWorkers) {
        if (initialWorkers <= 0) {
            initialWorkers = 1;
        }
        this.scheduler = new HibridScheduler(initialWorkers, new EarlySchedulerMapping().generateMappings(initialWorkers), 100000000);
    }

    @Override
    protected void initWorkers(int n, int id) {
        System.out.println("n early: " + n);
        for (int i = 0; i < n; i++) {
            new EarlyWorker(i, ((HibridScheduler) this.scheduler).getAllQueues()[i]).start();
        }
    }

    protected void initLateWorkers(int n, int id, int partitions) {
        System.out.println("n late: " + n);
        for (int i = 0; i < n; i++) {
            new LateWorker(i, partitions).start();
        }
    }











    private class EarlyWorker extends Thread {
        private final int thread_id;
        private Queue<TOMMessage> reqs;

        public EarlyWorker(int id, Queue<TOMMessage> reqs) {
            this.thread_id = id;
            this.reqs = reqs;
        }

        @Override
        public void run() {
            while (true) {
                TOMMessage request = reqs.poll();
                if (request != null) {
                    HibridClassToThreads ct = ((HibridScheduler) scheduler).getClass(request.getGroupId());

                    if (ct.type == HibridClassToThreads.CONC) {
                        MultiOperationRequest reqs = new MultiOperationRequest(request.getContent());
                        MultiOperationCtx ctx = new MultiOperationCtx(reqs.operations.length, request);
                        for (int i = 0; i < reqs.operations.length; i++) {

                            subgraphs[thread_id].insert(new HibridLockFreeNode(
                                    new MessageContextPair(request, request.groupId, i, reqs.operations[i], reqs.opId, ctx),
                                    Vertex.MESSAGE, subgraphs[thread_id], subgraphs.length, 0), false, false);
                        }
                    } else if (ct.type == HibridClassToThreads.SYNC) {
                        TOMMessageWrapper mw = (TOMMessageWrapper) request;

                        if (mw.msg.threadId == thread_id) {
                            mw.msg.node.graph = subgraphs[thread_id];
                            subgraphs[thread_id].insert(mw.msg.node, false, true);
                        } else {
                            subgraphs[thread_id].insert(mw.msg.node, true, true);
                        }
                    }
                }
            }
        }
    }

    private class LateWorker extends Thread {
        private final int thread_id;
        private final int myPartition;

        public LateWorker(int id, int partitions) {
            this.thread_id = id;
            this.myPartition = this.thread_id % partitions;
        }

        public byte[] serialize(short opId, short value) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 DataOutputStream oos = new DataOutputStream(baos)) {
                oos.writeShort(opId);
                oos.writeShort(value);
                oos.flush();
                return baos.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public void run() {
            MessageContextPair msg = null;
            while (true) {
                try {
                    //get
                    HibridLockFreeNode node = subgraphs[this.myPartition].get();
                    //execute
                    msg = (MessageContextPair) node.getData();
                    msg.resp = ((SingleExecutable) executor).executeOrdered(serialize(msg.opId, msg.operation), null);

                    msg.ctx.add(msg.index, msg.resp);
                    if (msg.ctx.response.isComplete() && !msg.ctx.finished && (msg.ctx.interger.getAndIncrement() == 0)) {
                        msg.ctx.finished = true;
                        msg.ctx.request.reply = new TOMMessage(id, msg.ctx.request.getSession(), msg.ctx.request.getSequence(), msg.ctx.response.serialize(), SVController.getCurrentViewId());
                        replier.manageReply(msg.ctx.request, null);
                    }
                    statistics.computeStatistics(thread_id, 1);
                    //remove
                    subgraphs[this.myPartition].remove(node);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }

    }
}
