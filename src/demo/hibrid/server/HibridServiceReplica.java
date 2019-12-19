/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.leaderchange.CertifiedDecision;
import bftsmart.tom.util.TOMUtil;
import demo.hibrid.request.CommandResult;
import demo.hibrid.request.Request;
import demo.hibrid.request.Response;
import demo.hibrid.server.graph.COSManager;
import demo.hibrid.server.scheduler.EarlyScheduler;
import demo.hibrid.server.scheduler.LateScheduler;
import demo.hibrid.server.queue.QueuesManager;
import demo.hibrid.stats.Stats;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;

/**
 * @author aldenio
 */
public class HibridServiceReplica extends ServiceReplica implements HibridReplier {

    private final EarlyScheduler earlyScheduler;
    private final QueuesManager queuesManager;
    private final LateScheduler[] lateSchedulers;
    private final COSManager cosManager;
    private final HibridWorker[] workers;
    private final HibridExecutor executor;
    private final Map<Integer, HibridRequestContext> context = new HashMap<>();

    public HibridServiceReplica(int id, EarlyScheduler earlyScheduler, QueuesManager queuesManager, COSManager cosManager, HibridExecutor executor, int numSchedulers, int numWorkers) {
        super(id, executor, null);
        this.earlyScheduler = earlyScheduler;
        this.queuesManager = queuesManager;
        this.cosManager = cosManager;
        this.executor = executor;
        this.workers = new HibridWorker[numWorkers];
        this.lateSchedulers = new LateScheduler[numSchedulers];
    }

    private void processOrderedRequest(TOMMessage message) {
        var request = new Request().fromBytes(message.getContent());
        var requestId = request.getId();
        var commands = request.getCommands();
        context.put(requestId, new HibridRequestContext(commands.length, message));
        Stats.messageReceive(request);
        earlyScheduler.schedule(request.getId(), request.getCommands());
        Stats.messageScheduled(request);
    }

    public void manageReply(ServerCommand serverCommand, boolean[] results) {
        var ctx = context.get(serverCommand.requestId);
        var commandResult = new CommandResult(serverCommand.commandId, results);
        ctx.add(commandResult);
        if (ctx.testFinished()) {
            var response = new Response(serverCommand.requestId, ctx.getResults());
            ctx.message.reply = new TOMMessage(id, ctx.message.getSession(), ctx.message.getSequence(), response.toBytes(), SVController.getCurrentViewId());
            replier.manageReply(ctx.message, null);
            Stats.messageReply(serverCommand.requestId);
        }
    }

    /////////////////////////////////////////////////////////////////////
    // Métodos para criar e executar as threads dos schedulers e workes.
    ////////////////////////////////////////////////////////////////////
    public void start() {
        initLateSchedulers();
        initReplicaWorkers();
    }

    private void initLateSchedulers() {
        for (int i = 0; i < lateSchedulers.length; i++) {
            lateSchedulers[i] = new LateScheduler(cosManager, queuesManager, i, i % queuesManager.getNumQueues(), i % cosManager.getNumCOS());
            lateSchedulers[i].start();
        }
    }

    private void initReplicaWorkers() {
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new HibridWorker(id, i, i % cosManager.getNumCOS(), cosManager, executor, this);
            workers[i].start();
        }
    }

    ////////////////////////////////////////////////////////////
    // OS MÉTODOS ABAIXO SÓ FAZEM A LIGAÇÃO COM O BFT-SMART
    ////////////////////////////////////////////////////////////

    private boolean processTOMMessage(TOMMessage request) {
        boolean noop = true;
        if (request.getViewID() == SVController.getCurrentViewId()) {
            switch (request.getReqType()) {
                case RECONFIG:
                    SVController.enqueueUpdate(request);
                    break;
                case ORDERED_REQUEST: {
                    noop = false;
                    processOrderedRequest(request);
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
        return noop;
    }

    @Override
    public void receiveMessages(int consId[], int regencies[], int leaders[], CertifiedDecision[] cDecs, TOMMessage[][] requests) {
        int consensusCount = 0;
        for (TOMMessage[] requestsFromConsensus : requests) {
            TOMMessage firstRequest = requestsFromConsensus[0];
            boolean noop = true;
            for (TOMMessage request : requestsFromConsensus) {
                bftsmart.tom.util.Logger.println("(ServiceReplica.receiveMessages) Processing TOMMessage from client " + request.getSender() + " with sequence number " + request.getSequence() + " for session " + request.getSession() + " decided in consensus " + consId[consensusCount]);
                noop &= processTOMMessage(request);
            }
            if (noop && this.recoverer != null) {
                processNoop(consId, regencies, leaders, cDecs, consensusCount, requestsFromConsensus, firstRequest);
            }
            consensusCount++;
        }
        if (SVController.hasUpdates()) {
            throw new UnsupportedOperationException(); // antigo => this.scheduler.scheduleReplicaReconfiguration();
        }
    }

    private void processNoop(int[] consId, int[] regencies, int[] leaders, CertifiedDecision[] cDecs, int consensusCount, TOMMessage[] requestsFromConsensus, TOMMessage firstRequest) {
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

    public CountDownLatch getReconfBarrier() throws BrokenBarrierException {
        throw new UnsupportedOperationException();
    }
}
