/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.list;

import bftsmart.util.MultiOperationRequest;

import java.io.IOException;

/**
 * @author alchieri
 */
public class BFTListMOMP<V> extends BFTList<V> {


    public BFTListMOMP(int id, boolean parallelExecution) {
        super(id, parallelExecution);
    }

    public boolean addP1(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.W1);
    }

    public boolean addP2(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.W2);
    }

    public boolean addP3(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.W3);
    }

    public boolean addP4(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.W4);
    }

    public boolean addP5(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.W5);
    }

    public boolean addP6(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.W6);
    }

    public boolean addP7(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.W7);
    }

    public boolean addP8(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.W8);
    }

    public boolean addAll(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.GW);
    }

    public boolean containsP1(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.R1);
    }

    public boolean containsP2(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.R2);
    }

    public boolean containsP3(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.R3);
    }

    public boolean containsP4(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.R4);
    }

    public boolean containsP5(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.R5);
    }

    public boolean containsP6(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.R6);
    }

    public boolean containsP7(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.R7);
    }

    public boolean containsP8(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.R8);
    }

    public boolean containsAll(V[] e) {
        return invokeMultiOperationRequestInParallel(e, MultipartitionMapping.GR);
    }


    private boolean invokeMultiOperationRequestInParallel(V[] e, int pId) {
        try {
            MultiOperationRequest mo = new MultiOperationRequest(e.length);
            for (int i = 0; i < e.length; i++) {
                var command = convertToBytes(pId, e[i]);
                mo.add(i, command, pId);
            }
            proxy.invokeParallel(mo.serialize(), -100);
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
