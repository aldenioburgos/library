/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.list.hibrid;

import bftsmart.tom.ParallelServiceProxy;
import parallelism.ParallelMapping;

import java.io.*;


/**
 * @author aldenio
 */
public class BFTListHibrid<V> {

    private ParallelServiceProxy proxy;

    public BFTListHibrid(int id) throws IOException {
        this.proxy = new ParallelServiceProxy(id);
    }

    public V get(int index) throws IOException, ClassNotFoundException {
        byte[] command = convertToBytes(ListCommand.GET.value, index);
        byte[] rep = proxy.invokeParallel(command, ParallelMapping.CONC_ALL);
        return (rep == null) ? null : convertToObject(rep);
    }

    public boolean set(V o) throws IOException {
        byte[] command = convertToBytes(ListCommand.SET.value, o);
        byte[] rep = proxy.invokeParallel(command, ParallelMapping.SYNC_ALL);
        return convertToBoolean(rep);
    }

    /*
     * Métodos auxiliares
     */
    private V convertToObject(byte[] data) throws IOException, ClassNotFoundException {
        try (var bis = new ByteArrayInputStream(data);
             var in = new ObjectInputStream(bis)) {
            return (V) in.readObject();
        }
    }

    private boolean convertToBoolean(byte[] data) throws IOException {
        try (var bis = new ByteArrayInputStream(data);
             var in = new ObjectInputStream(bis)) {
            return in.readBoolean();
        }
    }

    private byte[] convertToBytes(int command, int index) throws IOException {
        try (var out = new ByteArrayOutputStream();
             var dos = new ObjectOutputStream(out)) {
            dos.writeInt(command);
            dos.writeInt(index);
            return out.toByteArray();
        }
    }

    private byte[] convertToBytes(int command, Object o) throws IOException {
        try (var out = new ByteArrayOutputStream();
             var oos = new ObjectOutputStream(out)) {
            oos.writeInt(command);
            oos.writeObject(o);
            return out.toByteArray();
        }
    }
}
