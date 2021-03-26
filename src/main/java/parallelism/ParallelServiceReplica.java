/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parallelism;

import bftsmart.consensus.messages.MessageFactory;
import bftsmart.consensus.roles.Acceptor;
import bftsmart.consensus.roles.Proposer;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.ExecutionManager;
import bftsmart.tom.core.ParallelTOMLayer;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.leaderchange.CertifiedDecision;
import bftsmart.tom.server.Executable;
import bftsmart.tom.server.Recoverable;
import bftsmart.tom.server.SingleExecutable;
import bftsmart.tom.util.ShutdownHookThread;
import bftsmart.tom.util.TOMUtil;
import bftsmart.util.ThroughputStatistics;
import parallelism.scheduler.DefaultScheduler;
import parallelism.scheduler.Scheduler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alchieri
 */
public class ParallelServiceReplica extends ServiceReplica {

    protected Scheduler scheduler;
    public ThroughputStatistics statistics;

    public ParallelServiceReplica(int id, Executable executor, Recoverable recoverer, int initialWorkers) {
        super(id, executor, recoverer);
        createScheduler(initialWorkers);
        this.statistics = new ThroughputStatistics(id, initialWorkers, "resultsEarly_" + id + "_" + 1 + "_" + initialWorkers + ".txt", "");
        initWorkers(initialWorkers, id);
    }

    public ParallelServiceReplica(int id, Executable executor, Recoverable recoverer, Scheduler s) {
        super(id, executor, recoverer);

        this.scheduler = s;
        initWorkers(this.scheduler.getNumWorkers(), id);
    }

    public ParallelServiceReplica(int id, Executable executor, Recoverable recoverer, int initialWorkers, ClassToThreads[] cts) {
        super(id, executor, recoverer);

        if (initialWorkers <= 0) {
            initialWorkers = 1;
        }

        this.scheduler = new DefaultScheduler(initialWorkers, cts);

        initWorkers(this.scheduler.getNumWorkers(), id);
    }

    protected void createScheduler(int initialWorkers) {
        if (initialWorkers <= 0) {
            initialWorkers = 1;
        }
        int[] ids = new int[initialWorkers];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = i;
        }
        ClassToThreads[] cts = new ClassToThreads[2];
        cts[0] = new ClassToThreads(ParallelMapping.CONC_ALL, ClassToThreads.CONC, ids);
        cts[1] = new ClassToThreads(ParallelMapping.SYNC_ALL, ClassToThreads.SYNC, ids);
        this.scheduler = new DefaultScheduler(initialWorkers, cts);

    }


    protected void initWorkers(int n, int id) {
        int tid = 0;
        for (int i = 0; i < n; i++) {
            new ServiceReplicaWorker(this.scheduler.getMapping().getAllQueues()[i], tid).start();
            tid++;
        }
    }

    public int getNumActiveThreads() {
        return this.scheduler.getMapping().getNumWorkers();
    }

    /**
     * Barrier used to reconfigure the number of replicas in the system
     *
     * @return
     */
    public CyclicBarrier getReconfBarrier() {
        return this.scheduler.getMapping().getReconfBarrier();
    }

    @Override
    public void receiveMessages(int consId[], int regencies[], int leaders[], CertifiedDecision[] cDecs, TOMMessage[][] requests) {
        int consensusCount = 0;
        boolean noop = true;

        for (TOMMessage[] requestsFromConsensus : requests) {
            TOMMessage firstRequest = requestsFromConsensus[0];
            noop = true;
            for (TOMMessage request : requestsFromConsensus) {

                bftsmart.tom.util.Logger.println("(ServiceReplica.receiveMessages) Processing TOMMessage from client " + request.getSender() + " with sequence number " + request.getSequence() + " for session " + request.getSession() + " decided in consensus " + consId[consensusCount]);

                if (request.getViewID() == SVController.getCurrentViewId()) {
                    if (request.getReqType() == TOMMessageType.ORDERED_REQUEST) {
                        noop = false;
                        statistics.start();
                        this.scheduler.schedule(request);

                    } else if (request.getReqType() == TOMMessageType.RECONFIG) {

                        SVController.enqueueUpdate(request);
                    } else {
                        throw new RuntimeException("Should never reach here! ");
                    }

                } else if (request.getViewID() < SVController.getCurrentViewId()) {
                    // message sender had an old view, resend the message to
                    // him (but only if it came from consensus an not state transfer)
                    tomLayer.getCommunication().send(new int[]{request.getSender()}, new TOMMessage(SVController.getStaticConf().getProcessId(),
                            request.getSession(), request.getSequence(), TOMUtil.getBytes(SVController.getCurrentView()), SVController.getCurrentViewId()));

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

    /**
     * This method initializes the object
     *
     */
    private void initTOMLayer() {
        if (tomStackCreated) { // if this object was already initialized, don't do it again
            return;
        }

        if (!SVController.isInCurrentView()) {
            throw new RuntimeException("I'm not an acceptor!");
        }

        // Assemble the total order messaging layer
        MessageFactory messageFactory = new MessageFactory(id);

        Acceptor acceptor = new Acceptor(cs, messageFactory, SVController);
        cs.setAcceptor(acceptor);

        Proposer proposer = new Proposer(cs, messageFactory, SVController);

        ExecutionManager executionManager = new ExecutionManager(SVController, acceptor, proposer, id);

        acceptor.setExecutionManager(executionManager);

        tomLayer = new ParallelTOMLayer(executionManager, this, recoverer, acceptor, cs, SVController, verifier);

        executionManager.setTOMLayer(tomLayer);

        SVController.setTomLayer(tomLayer);

        cs.setTOMLayer(tomLayer);
        cs.setRequestReceiver(tomLayer);

        acceptor.setTOMLayer(tomLayer);

        if (SVController.getStaticConf().isShutdownHookEnabled()) {
            Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(tomLayer));
        }
        tomLayer.start(); // start the layer execution
        tomStackCreated = true;

        replicaCtx = new ReplicaContext(cs, SVController);
    }

    private class ServiceReplicaWorker extends Thread {

        private Queue<MessageContextPair> requests;
        private int thread_id;

        public ServiceReplicaWorker(Queue<MessageContextPair> requests, int id) {
            this.thread_id = id;
            this.requests = requests;
        }

        int localC = 0;
        int localTotal = 0;

        public byte[] serialize(short opId, short value) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream oos = new DataOutputStream(baos);

                oos.writeShort(opId);

                oos.writeShort(value);

                oos.close();
                return baos.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private void execute(MessageContextPair msg) {
            msg.resp = ((SingleExecutable) executor).executeOrdered(serialize(msg.opId, msg.operation), null);
            msg.ctx.add(msg.index, msg.resp);
            if (msg.ctx.response.isComplete() && !msg.ctx.finished && (msg.ctx.interger.getAndIncrement() == 0)) {
                msg.ctx.finished = true;
                msg.ctx.request.reply = new TOMMessage(id, msg.ctx.request.getSession(),
                        msg.ctx.request.getSequence(), msg.ctx.response.serialize(), SVController.getCurrentViewId());
                replier.manageReply(msg.ctx.request, null);
            }
            statistics.computeStatistics(thread_id, 1);
        }

        public void run() {

            while (true) {

                MessageContextPair msg = requests.poll();
                if (msg != null) {
                    try {
                        //EDUARDO: Não funciona com reconfiguração no conjunto de replicas, precisa colocar uma classe para isso em ClassToThreads com type = REC.
                        ClassToThreads ct = scheduler.getMapping().getClass(msg.classId);

                        if (ct.type == ClassToThreads.CONC) {
                            execute(msg);

                        } else if (ct.type == ClassToThreads.SYNC && ct.tIds.length == 1) {//SYNC mas só com 1 thread, não precisa usar barreira
                            execute(msg);
                        } else if (ct.type == ClassToThreads.SYNC) {

                            if (thread_id == scheduler.getMapping().getExecutorThread(msg.classId)) {
                                scheduler.getMapping().getBarrier(msg.classId).await();
                                execute(msg);
                                scheduler.getMapping().getBarrier(msg.classId).await();

                            } else {
                                scheduler.getMapping().getBarrier(msg.classId).await();
                                scheduler.getMapping().getBarrier(msg.classId).await();
                            }

                        } else if (msg.classId == ParallelMapping.CONFLICT_RECONFIGURATION) {
                            scheduler.getMapping().getReconfBarrier().await();
                            scheduler.getMapping().getReconfBarrier().await();

                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ParallelServiceReplica.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(ParallelServiceReplica.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

            }
        }
    }

}
