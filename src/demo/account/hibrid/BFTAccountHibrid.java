/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.account.hibrid;

import bftsmart.tom.ParallelServiceProxy;
import demo.account.hibrid.commands.AccountCommand;
import demo.account.hibrid.commands.AccountResponse;

import java.io.*;


/**
 * @author aldenio
 */
public class BFTAccountHibrid {

    private ParallelServiceProxy proxy;

    public BFTAccountHibrid(int id) {
        this.proxy = new ParallelServiceProxy(id);
    }

    public AccountResponse[] execute(AccountCommand... accountCommand) {
        try {
            byte[] commandBytes = convertToBytes(accountCommand);
            byte[] rep = proxy.invokeParallel(commandBytes, 0);
            return (rep == null) ? null : convertToResponse(rep);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Métodos auxiliares
     */
    private AccountResponse[] convertToResponse(byte[] data) throws IOException, ClassNotFoundException {
        try (var bis = new ByteArrayInputStream(data);
             var in = new ObjectInputStream(bis)) {
            return (AccountResponse[]) in.readObject();
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
