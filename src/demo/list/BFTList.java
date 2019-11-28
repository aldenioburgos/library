/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.list;

import bftsmart.tom.ParallelServiceProxy;
import parallelism.ParallelMapping;

import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alchieri
 */
public class BFTList<V> implements List<V> {

    static final int CONTAINS = 1;
    static final int ADD = 2;
    static final int GET = 3;
    static final int REMOVE = 4;
    static final int SIZE = 5;

    protected ParallelServiceProxy proxy;
    protected boolean parallel;

    public BFTList(int id, boolean parallelExecution) {
        this.parallel = parallelExecution;
        this.proxy = new ParallelServiceProxy(id);
    }

    @Override
    public int size() {
        try {
            byte[] command = convertToBytes(SIZE);
            byte[] rep;
            if (parallel) {
                rep = proxy.invokeParallel(command, ParallelMapping.CONC_ALL);
            } else {
                rep = proxy.invokeOrdered(command);
            }
            return convertToInt(rep);
        } catch (IOException ex) {
            return -1;
        }
    }

    @Override
    public boolean add(V e) {
        try {
            byte[] rep;
            byte[] command = convertToBytes(ADD, e);
            if (parallel) {
                rep = proxy.invokeParallel(command, ParallelMapping.SYNC_ALL);
            } else {
                rep = proxy.invokeOrdered(command);
            }
            return convertToBoolean(rep);
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean remove(Object o) {
        try {
            byte[] rep;
            byte[] command = convertToBytes(REMOVE, o);
            if (parallel) {
                rep = proxy.invokeParallel(command, ParallelMapping.SYNC_ALL);
            } else {
                rep = proxy.invokeOrdered(command);
            }
           
            return convertToBoolean(rep);
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean contains(Object o) {
        try {
            byte[] rep;
            byte[] command = convertToBytes(CONTAINS, o);
            if (parallel) {
                rep = proxy.invokeParallel(command, ParallelMapping.CONC_ALL);
            } else {
                rep = proxy.invokeOrdered(command);
            }
            return convertToBoolean(rep);
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public V get(int index) {
        try {
            byte[] rep;
            byte[] command = convertToBytes(GET, index);
            if (parallel) {
                rep = proxy.invokeParallel(command, ParallelMapping.CONC_ALL);
            } else {
                rep = proxy.invokeOrdered(command);
            }
            if (rep == null) {
                return null;
            }
            return convertToObject(rep);
        } catch (IOException ex) {
            return null;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(BFTList.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }


    private V convertToObject(byte[] data) throws IOException, ClassNotFoundException {
        try (var bis = new ByteArrayInputStream(data);
             var in = new ObjectInputStream(bis)) {
            final V v = (V) in.readObject();
            return v;
        }
    }
        
    private boolean convertToBoolean(byte[] data) throws IOException {
        try (var bis = new ByteArrayInputStream(data);
             var in = new ObjectInputStream(bis)) {
            return in.readBoolean();
        }
    }
    
    private int convertToInt(byte[] data) throws IOException {
        try (var bis = new ByteArrayInputStream(data);
             var in = new ObjectInputStream(bis)) {
            return in.readInt();
        }
    }


    private byte[] convertToBytes(int command) throws IOException {
        try (var out = new ByteArrayOutputStream();
             var dos = new ObjectOutputStream(out)) {
            dos.writeInt(command);
            return out.toByteArray();
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

    protected byte[] convertToBytes(int command, Object o) throws IOException {
        try (var out = new ByteArrayOutputStream();
             var oos = new ObjectOutputStream(out)) {
            oos.writeInt(command);
            oos.writeObject(o);
            return out.toByteArray();
        }
    }


    /*
     * Métodos herdados da interface LIST que não fazem sentido nesse contexto.
     */


    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterator<V> iterator() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean addAll(int index, Collection<? extends V> c) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public V set(int index, V element) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void add(int index, V element) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public V remove(int index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ListIterator<V> listIterator() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ListIterator<V> listIterator(int index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<V> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
