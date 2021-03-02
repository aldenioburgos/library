package bftsmart.util;

import java.io.*;

/**
 * @author eduardo
 */
public class MultiOperationRequest {

    public short[] operations;
    public short opId;

    public MultiOperationRequest(int number, short opId) {
        this.operations = new short[number];
        this.opId = opId;
    }

    public void add(int index, short data) {
        this.operations[index] = data;
    }

    public MultiOperationRequest(byte[] buffer) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(buffer);
             DataInputStream dis = new DataInputStream(in)) {
            this.opId = dis.readShort();
            this.operations = new short[dis.readShort()];
            for (int i = 0; i < this.operations.length; i++) {
                this.operations[i] = dis.readShort();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public byte[] serialize() {
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream oos = new DataOutputStream(baos)) {
            oos.writeShort(opId);
            oos.writeShort(operations.length);
            for (int i = 0; i < operations.length; i++) {
                oos.writeShort(this.operations[i]);
            }
            oos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getNumOperations() {
        return operations.length;
    }

}
