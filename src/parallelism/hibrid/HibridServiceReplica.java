/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parallelism.hibrid;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.leaderchange.CertifiedDecision;
import bftsmart.tom.server.Executable;
import bftsmart.tom.server.Recoverable;
import bftsmart.tom.server.SingleExecutable;
import bftsmart.tom.util.TOMUtil;
import bftsmart.util.MultiOperationRequest;
import bftsmart.util.ThroughputStatistics;
import parallelism.*;
import parallelism.scheduler.DefaultScheduler;
import parallelism.scheduler.Scheduler;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;


/**
 * @author aldenio
 */
public class HibridServiceReplica extends ServiceReplica {

    public ThroughputStatistics statistics;
    protected Scheduler scheduler;
    protected Map<String, MultiOperationCtx> ctxs = new Hashtable<>();

    public HibridServiceReplica(int id, Executable executor, Recoverable recoverer, int numPartitions, int initialWorkers) {
        super(id, executor, recoverer);
        if (initialWorkers <= 0) {
            initialWorkers = 1;
        }
        int[] ids = createArrayOfIndexes(initialWorkers);

        ClassToThreads[] cts = new ClassToThreads[2];
        cts[0] = new ClassToThreads(ParallelMapping.CONC_ALL, ClassToThreads.CONC, ids);
        cts[1] = new ClassToThreads(ParallelMapping.SYNC_ALL, ClassToThreads.SYNC, ids);
        this.scheduler = new DefaultScheduler(initialWorkers, cts);
        initWorkers(this.scheduler.getNumWorkers(), id);
    }

    private int[] createArrayOfIndexes(int numOfIndexes) {
        int[] ids = new int[numOfIndexes];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = i;
        }
        return ids;
    }

    public HibridServiceReplica(int id, Executable executor, Recoverable recoverer, Scheduler scheduler) {
        super(id, executor, recoverer);
        this.scheduler = scheduler;
        initWorkers(this.scheduler.getNumWorkers(), id);
    }

    public HibridServiceReplica(int id, Executable executor, Recoverable recoverer, int initialWorkers, ClassToThreads[] cts) {
        super(id, executor, recoverer);
        if (initialWorkers <= 0) {
            initialWorkers = 1;
        }
        this.scheduler = new DefaultScheduler(initialWorkers, cts);
        initWorkers(this.scheduler.getNumWorkers(), id);
    }

    protected void initWorkers(int numWorkers, int idReplica) {
        statistics = new ThroughputStatistics(idReplica, numWorkers, "results_" + idReplica + ".txt", "");
        int tid = 0;
        for (int i = 0; i < numWorkers; i++) {
            new ServiceReplicaWorker((FIFOQueue) this.scheduler.getMapping().getAllQueues()[i], tid).start();
            tid++;
        }
    }

    /**
     * Barrier used to reconfigure the number of replicas in the system
     */
    public CyclicBarrier getReconfBarrier() {
        return this.scheduler.getMapping().getReconfBarrier();
    }

    @Override
    public void receiveMessages(int consId[], int regencies[], int leaders[], CertifiedDecision[] cDecs, TOMMessage[][] requests) {

        ctxs.keySet().removeIf(key -> ctxs.get(key).finished);

        int consensusCount = 0;

        for (TOMMessage[] requestsFromConsensus : requests) {
            TOMMessage firstRequest = requestsFromConsensus[0];
            boolean noop = true;
            for (TOMMessage request : requestsFromConsensus) {

                bftsmart.tom.util.Logger.println("(ServiceReplica.receiveMessages) Processing TOMMessage from client " + request.getSender() + " with sequence number " + request.getSequence() + " for session " + request.getSession() + " decided in consensus " + consId[consensusCount]);

                if (request.getViewID() == SVController.getCurrentViewId()) {
                    switch (request.getReqType()) {
                        case RECONFIG:
                            SVController.enqueueUpdate(request);
                            break;
                        case ORDERED_REQUEST: {
                            noop = false;
                            MultiOperationRequest reqs = new MultiOperationRequest(request.getContent());
                            MultiOperationCtx ctx = new MultiOperationCtx(reqs.operations.length, request);
                            this.ctxs.put(request.toString(), ctx);
                            statistics.start();
                            for (int i = 0; i < reqs.operations.length; i++) {
                                this.scheduler.schedule(new MessageContextPair(request, reqs.operations[i].classId, i, reqs.operations[i].data));
                            }
                            break;
                        }
                        default:
                            throw new RuntimeException("Unknown Request Type.");
                    }
                } else if (request.getViewID() < SVController.getCurrentViewId()) {
                    // message sender had an old view, resend the message to
                    // him (but only if it came from consensus an not state transfer)
                    var sender = SVController.getStaticConf().getProcessId();
                    var content = TOMUtil.getBytes(SVController.getCurrentView());
                    var view = SVController.getCurrentViewId();
                    var targets = new int[]{request.getSender()};
                    tomLayer.getCommunication().send(targets, new TOMMessage(sender, request.getSession(), request.getSequence(), content, view));
                }
            }

            // This happens when a consensus finishes but there are no requests to deliver
            // to the application. This can happen if a reconfiguration is issued and is the only
            // operation contained in the batch. The recoverer must be notified about this,
            // hence the invocation of "noop"
            if (noop && this.recoverer != null) {

                bftsmart.tom.util.Logger.println("(ServiceReplica.receiveMessages) Delivering a no-op to the recoverer");

                System.out.println(" --- A consensus instance finished, but there were no commands to deliver to the application.");
                System.out.println(" --- Notifying recoverable about a blank consensus.");

                byte[][] batch = null;
                MessageContext[] msgCtx = null;
                if (requestsFromConsensus.length > 0) {
                    //Make new batch to deliver
                    batch = new byte[requestsFromConsensus.length][];
                    msgCtx = new MessageContext[requestsFromConsensus.length];

                    //Put messages in the batch
                    int line = 0;
                    for (TOMMessage m : requestsFromConsensus) {
                        batch[line] = m.getContent();

                        msgCtx[line] = new MessageContext(m.getSender(), m.getViewID(),
                                m.getReqType(), m.getSession(), m.getSequence(), m.getOperationId(),
                                m.getReplyServer(), m.serializedMessageSignature, firstRequest.timestamp,
                                m.numOfNonces, m.seed, regencies[consensusCount], leaders[consensusCount],
                                consId[consensusCount], cDecs[consensusCount].getConsMessages(), firstRequest, true);
                        msgCtx[line].setLastInBatch();

                        line++;
                    }
                }
                this.recoverer.noOp(consId[consensusCount], batch, msgCtx);
            }
            consensusCount++;
        }
        if (SVController.hasUpdates()) {
            this.scheduler.scheduleReplicaReconfiguration();
        }
    }




    private class ServiceReplicaWorker extends Thread {
        private FIFOQueue<MessageContextPair> requests;
        private int thread_id;

        public ServiceReplicaWorker(FIFOQueue<MessageContextPair> requests, int id) {
            this.thread_id = id;
            this.requests = requests;
            this.setName("ServiceReplicaWorker[" + id + "]");
        }

        public void run() {
            var execQueue = new ExecutionFIFOQueue<MessageContextPair>();
            while (true) {
                try {
                    this.requests.drainToQueue(execQueue);
                    System.out.println("Thread " + thread_id + ": " + execQueue.getSize());

                    do {
                        MessageContextPair msg = execQueue.getNext();
                        //TODO: EDUARDO: Não funciona com reconfiguração no conjunto de replicas, precisa colocar uma classe para isso em ClassToThreads com type = REC.
                        ClassToThreads ct = scheduler.getMapping().getClass(msg.classId);
                        switch (ct.type) {
                            case ClassToThreads.CONC:
                                runOperation(msg, thread_id);
                                break;
                            case ClassToThreads.SYNC: {
                                if (ct.tIds.length == 1) { //SYNC mas só com 1 thread, não precisa usar barreira
                                    runOperation(msg, thread_id);
                                } else if (thread_id == scheduler.getMapping().getExecutorThread(msg.classId)) {
                                    scheduler.getMapping().getBarrier(msg.classId).await();
                                    runOperation(msg, thread_id);
                                    scheduler.getMapping().getBarrier(msg.classId).await();
                                } else {
                                    scheduler.getMapping().getBarrier(msg.classId).await();
                                    //TODO: esse trecho sem nada está estranho!
                                    scheduler.getMapping().getBarrier(msg.classId).await();
                                }
                                break;
                            }
                            default: {
                                if (msg.classId == ParallelMapping.CONFLICT_RECONFIGURATION) {
                                    scheduler.getMapping().getReconfBarrier().await();
                                    //TODO: esse trecho sem nada está estranho!
                                    scheduler.getMapping().getReconfBarrier().await();
                                }
                                break;
                            }
                        }
                    } while (execQueue.goToNext());
                } catch (BrokenBarrierException | InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    protected void runOperation(MessageContextPair msg, int thread_id) {
        msg.resp = ((SingleExecutable) executor).executeOrdered(msg.operation, null);
        manageReply(msg);
        statistics.computeStatistics(thread_id, 1);
    }

    protected void manageReply(MessageContextPair msg) {
        MultiOperationCtx ctx = ctxs.get(msg.request.toString());
        ctx.add(msg.index, msg.resp);
        if (ctx.response.isComplete() && !ctx.finished && (ctx.interger.getAndIncrement() == 0)) {
            ctx.finished = true;
            ctx.request.reply = new TOMMessage(id, ctx.request.getSession(), ctx.request.getSequence(), ctx.response.serialize(), SVController.getCurrentViewId());
            replier.manageReply(ctx.request, null);
        }
    }
}
