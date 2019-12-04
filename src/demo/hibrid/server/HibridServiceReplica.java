/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.server;

import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.Executable;
import bftsmart.tom.server.Recoverable;
import demo.hibrid.request.Command;
import demo.hibrid.request.CommandResult;
import demo.hibrid.request.Request;
import demo.hibrid.server.scheduler.EarlyScheduler;
import demo.hibrid.server.scheduler.LateScheduler;
import parallelism.ParallelServiceReplica;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author eduardo
 */
public class HibridServiceReplica extends ParallelServiceReplica {

    private final EarlyScheduler earlyScheduler;
    private final LateScheduler[] lateSchedulers;
    private final Map<Integer, Map<Integer, CommandResult>> context = new HashMap<>(); //TODO avalidar se essa é a melhor forma para o contexto.

    public HibridServiceReplica(int id, Executable executor, Recoverable recoverer, int numPartitions, int numWorkers) {
        super(id, executor, recoverer);
        this.earlyScheduler = new EarlyScheduler(numPartitions);
        this.lateSchedulers = new LateScheduler[numWorkers];
    }


    protected void processOrderedRequest(TOMMessage message) {
        var request = new Request().fromBytes(message.getContent());
        var requestId = request.getId();
        var resultMap = new HashMap<Integer,CommandResult>();
        context.put(requestId, resultMap);
        for (Command command: request.getCommands()) {
            earlyScheduler.schedule(new ServerCommand(requestId, command));
            resultMap.put(command.getId(), null);
        }
    }



}
