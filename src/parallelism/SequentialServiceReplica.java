/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parallelism;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.leaderchange.CertifiedDecision;
import bftsmart.tom.server.Executable;
import bftsmart.tom.server.Recoverable;
import bftsmart.tom.server.SingleExecutable;
import bftsmart.tom.util.TOMUtil;
import bftsmart.util.MultiOperationRequest;
import bftsmart.util.ThroughputStatistics;
import parallelism.scheduler.Scheduler;

import java.util.Hashtable;
import java.util.Map;

/**
 * @author alchieri
 */
public class SequentialServiceReplica extends ServiceReplica {

    protected Scheduler scheduler;
    public ThroughputStatistics statistics;

    protected Map<String, MultiOperationCtx> ctxs = new Hashtable<>();

    public SequentialServiceReplica(int id, Executable executor, Recoverable recoverer) {
        super(id, executor, recoverer);
        statistics = new ThroughputStatistics(id, 1, "results_" + id + ".txt", "");
    }

    @Override
    public void receiveMessages(int consId[], int regencies[], int leaders[], CertifiedDecision[] cDecs, TOMMessage[][] requests) {
        // remove finished
        ctxs.keySet().removeIf(key -> ctxs.get(key).finished);

        int consensusCount = 0;
        boolean noop;

        for (TOMMessage[] requestsFromConsensus : requests) {
            TOMMessage firstRequest = requestsFromConsensus[0];
            noop = true;
            for (TOMMessage request : requestsFromConsensus) {

                bftsmart.tom.util.Logger.println("(ServiceReplica.receiveMessages) Processing TOMMessage from client " + request.getSender() + " with sequence number " + request.getSequence() + " for session " + request.getSession() + " decided in consensus " + consId[consensusCount]);

                if (request.getViewID() == SVController.getCurrentViewId()) {
                    if (request.getReqType() == TOMMessageType.ORDERED_REQUEST) {
                        noop = false;
                        MultiOperationRequest reqs = new MultiOperationRequest(request.getContent());
                        MultiOperationCtx ctx = new MultiOperationCtx(reqs.operations.length, request);
                        statistics.start();
                        for (int i = 0; i < reqs.operations.length; i++) {
                            MessageContextPair msg = new MessageContextPair(request, reqs.operations[i].classId, i, reqs.operations[i].data);
                            msg.resp = ((SingleExecutable) executor).executeOrdered(msg.operation, null);
                            ctx.add(msg.index, msg.resp);
                            statistics.computeStatistics(0, 1);
                        }
                        ctx.request.reply = new TOMMessage(id, ctx.request.getSession(), ctx.request.getSequence(), ctx.response.serialize(), SVController.getCurrentViewId());
                        replier.manageReply(ctx.request, null);
                    } else if (request.getReqType() == TOMMessageType.RECONFIG) {
                        SVController.enqueueUpdate(request);
                    } else {
                        throw new RuntimeException("Should never reach here! ");
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

            //System.out.println("BATCH SIZE: "+requestCount);
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

                //MessageContext msgCtx = new MessageContext(-1, -1, null, -1, -1, -1, -1, null, // Since it is a noop, there is no need to pass info about the client...
                //        -1, 0, 0, regencies[consensusCount], leaders[consensusCount], consId[consensusCount], cDecs[consensusCount].getConsMessages(), //... but there is still need to pass info about the consensus
                //        null, true); // there is no command that is the first of the batch, since it is a noop
                //msgCtx.setLastInBatch();
                //this.recoverer.noOp(msgCtx.getConsensusId(), msgCtx);
            }

            consensusCount++;
        }
        if (SVController.hasUpdates()) {

            this.scheduler.scheduleReplicaReconfiguration();

        }
    }

    /*
     * This method initializes the object
     *
     * @param cs Server side communication System
     * @param conf Total order messaging configuration
     */
    /* private void initTOMLayer() {
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
    }*/


}
