/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.account.hibrid;

import bftsmart.tom.ParallelServiceProxy;
import demo.account.hibrid.commands.AccountCommand;
import demo.account.hibrid.commands.CheckBalance;
import demo.account.hibrid.commands.Transfer;
import parallelism.ParallelMapping;

import java.io.*;


/**
 * @author aldenio
 */
public class BFTAccountHibrid {

    private ParallelServiceProxy proxy;

    public BFTAccountHibrid(int id) throws IOException {
        this.proxy = new ParallelServiceProxy(id);
    }

    public AccountCommand execute(CheckBalance accountCommand) throws IOException, ClassNotFoundException {
        byte[] commandBytes = convertToBytes(accountCommand);
        byte[] rep = proxy.invokeParallel(commandBytes, ParallelMapping.CONC_ALL);
        return (rep == null) ? null : convertToCommand(rep);
    }

    public AccountCommand execute(Transfer accountCommand) throws IOException, ClassNotFoundException {
        byte[] commandBytes = convertToBytes(accountCommand);
        byte[] rep = proxy.invokeParallel(commandBytes, ParallelMapping.SYNC_ALL);
        return (rep == null) ? null : convertToCommand(rep);
    }

    /*
     * Métodos auxiliares
     */
    private AccountCommand convertToCommand(byte[] data) throws IOException, ClassNotFoundException {
        try (var bis = new ByteArrayInputStream(data);
             var in = new ObjectInputStream(bis)) {
            return (AccountCommand) in.readObject();
        }
    }
    private byte[] convertToBytes(Object o) throws IOException {
        try (var out = new ByteArrayOutputStream();
             var oos = new ObjectOutputStream(out)) {
            oos.writeObject(o);
            return out.toByteArray();
        }
    }
}
