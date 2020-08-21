/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.client;

import bftsmart.tom.ServiceProxy;
import demo.hibrid.request.CommandResult;
import demo.hibrid.request.Request;
import demo.hibrid.request.Response;


/**
 * @author aldenio
 */
public class BftAdapter {

    private final ServiceProxy proxy;
    private boolean ignoreAnswer;

    public BftAdapter(int id) {
        this.proxy = new ServiceProxy(id);
    }

    public BftAdapter(ServiceProxy proxy, boolean ignoreAnswer) {
        this.proxy = proxy;
        this.ignoreAnswer = ignoreAnswer;
    }

    public CommandResult[] execute(Request request) {
        byte[] requestBytes = request.toBytes();
        byte[] responseBytes = proxy.invokeOrdered(requestBytes);
        if (ignoreAnswer) return new CommandResult[0];
        else return new Response().fromBytes(responseBytes).getResults();
    }

}
