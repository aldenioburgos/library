/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parallelism;


import bftsmart.tom.core.messages.TOMMessage;
import parallelism.hibrid.late.HibridLockFreeNode;

/**
 * @author eduardo
 */
public class MessageContextPair implements HoldsClassIdInterface {
    // preenchido pelo construtor
    public TOMMessage request;
    public int classId;
    public int index;
    public short operation;
    public short opId;
    public MultiOperationCtx ctx;

    // preenchido posteriormente
    public byte[] resp;
    public HibridLockFreeNode node = null;
    public int threadId;

    public MessageContextPair(TOMMessage message, int classId, int index, short operation, short opId, MultiOperationCtx ctx) {
        this.request = message;
        this.classId = classId;
        this.index = index;
        this.operation = operation;
        this.opId = opId;
        this.ctx = ctx;
    }

    @Override
    public String toString() {
        return "request [class id= " + classId + ", operation id= " + opId + " requestID: " + request + " index: " + index + "]";
    }

    public int getClassId() {
        return classId;
    }
}
