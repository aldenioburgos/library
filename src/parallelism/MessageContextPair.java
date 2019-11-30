/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parallelism;


import bftsmart.tom.core.messages.TOMMessage;

/**
 * @author eduardo
 */
public class MessageContextPair {
    public TOMMessage request;
    public int classId;
    public int partitionId;
    public byte[] operation;
    public int index;
    public byte[] resp;

    public MessageContextPair(TOMMessage message, int classId, int index, byte[] operation) {
        this.request = message;
        this.classId = classId;
        this.index = index;
        this.operation = operation;
    }

    public MessageContextPair(TOMMessage request, int classId, int partitionId, int index, byte[] operation) {
        this.request = request;
        this.classId = classId;
        this.partitionId = partitionId;
        this.operation = operation;
        this.index = index;
    }
}
