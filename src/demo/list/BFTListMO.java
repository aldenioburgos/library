/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.list;


import bftsmart.util.MultiOperationRequest;
import parallelism.ParallelMapping;

import java.io.IOException;

/**
 * @author alchieri
 */
public class BFTListMO<V> extends BFTList<V> {

    public BFTListMO(int id, boolean parallelExecution) {
        super(id, parallelExecution);
    }

    public boolean add(V[] e) {
        try {
            MultiOperationRequest mo = new MultiOperationRequest(e.length);
            for (int i = 0; i < e.length; i++) {
                var command = convertToBytes(ADD, e[i]);
                mo.add(i, command, ParallelMapping.SYNC_ALL);
            }
            if (parallel) {
                proxy.invokeParallel(mo.serialize(), ParallelMapping.SYNC_ALL);
            } else {
                proxy.invokeOrdered(mo.serialize());
            }
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean contains(V[] e) {
        try {
            MultiOperationRequest mo = new MultiOperationRequest(e.length);
            for (int i = 0; i < e.length; i++) {
                var command = convertToBytes(CONTAINS, e[i]);
                mo.add(i, command, ParallelMapping.CONC_ALL);
            }
            if (parallel) {
                proxy.invokeParallel(mo.serialize(), ParallelMapping.CONC_ALL);
            } else {
                proxy.invokeOrdered(mo.serialize());
            }
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

}
