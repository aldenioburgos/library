/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.leaderchange.CertifiedDecision;
import bftsmart.tom.server.Executable;
import bftsmart.tom.server.Recoverable;
import bftsmart.tom.util.TOMUtil;
import demo.hibrid.request.Request;
import demo.hibrid.request.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author aldenio
 */
public abstract class AbstractServiceReplica extends ServiceReplica  {

    protected final Map<Integer, TOMMessage> msgStore= new ConcurrentHashMap<>();
    
    public AbstractServiceReplica(int id, Executable executor, Recoverable recoverer) {
        super(id, executor, recoverer);
    }

    protected void processOrderedRequest(TOMMessage message) {
        Request request = new Request().fromBytes(message.getContent());
        var requestId = request.getId();
        msgStore.put(requestId, message);
        processRequest(request);
    }

    protected abstract void processRequest(Request request);

    protected void reply(Response response) {
        TOMMessage message = msgStore.remove(response.getId());
        message.reply = new TOMMessage(id, message.getSession(),message.getSequence(), response.toBytes(), SVController.getCurrentViewId());
        replier.manageReply(message, null);
    }


    @Override
    public void receiveMessages(int[]  consId, int[] regencies, int[] leaders, CertifiedDecision[] cDecs, TOMMessage[][] requests) {
        int consensusCount = 0;
        for (TOMMessage[] requestsFromConsensus : requests) {
            TOMMessage firstRequest = requestsFromConsensus[0];
            boolean noop = true;
            for (TOMMessage request : requestsFromConsensus) {
                bftsmart.tom.util.Logger.println("(ServiceReplica.receiveMessages) Processing TOMMessage from client " + request.getSender() + " with sequence number " + request.getSequence() + " for session " + request.getSession() + " decided in consensus " + consId[consensusCount]);
                if (request.getViewID() == SVController.getCurrentViewId()) {
                    if (request.getReqType() == TOMMessageType.ORDERED_REQUEST) {
                        noop = false;
                        processOrderedRequest(request);
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
            }

            consensusCount++;
        }
        if (SVController.hasUpdates()) {
            // this.scheduler.scheduleReplicaReconfiguration(); //TODO isso aqui pode ficar assim?
            throw new UnsupportedOperationException();
        }
    }


}
