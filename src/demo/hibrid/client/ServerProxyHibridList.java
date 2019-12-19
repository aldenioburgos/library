/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.client;

import bftsmart.tom.ParallelServiceProxy;
import demo.hibrid.request.Command;
import demo.hibrid.request.CommandResult;
import demo.hibrid.request.Request;
import demo.hibrid.request.Response;


/**
 * @author aldenio
 */
public class ServerProxyHibridList implements ServerProxy {

    private ParallelServiceProxy proxy;

    public ServerProxyHibridList(int id) {
        this.proxy = new ParallelServiceProxy(id);
    }

    public CommandResult[] execute(int clientProcessId, int id, Command... commands) {
        var request = new Request(clientProcessId, id, commands);
        byte[] requestBytes = request.toBytes();
        byte[] rep = proxy.invokeParallel(requestBytes, 0);
        return (rep == null) ? null : new Response().fromBytes(rep).getResults();
    }

}
