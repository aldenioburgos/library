package demo.coin;

import bftsmart.util.MultiOperationRequest;
import demo.coin.core.transactions.CoinOperation;

import java.io.*;

public class CoinMultiOperationRequest extends MultiOperationRequest {

    private CoinOperation[] operations;

    public CoinMultiOperationRequest(byte[] buffer) {
        super(0, (short) 0); //TODO marreta pra funcionar com o codigo do prof.

        try (ByteArrayInputStream in = new ByteArrayInputStream(buffer);
             DataInputStream dis = new DataInputStream(in)) {
            opId = dis.readShort();
            operations = new CoinOperation[dis.readShort()];
            for (int i = 0; i < this.operations.length; i++) {
                operations[i] = CoinOperation.loadFrom(dis);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public byte[] serialize() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeShort(opId);
            dos.writeShort(operations.length);
            for (var op : operations) {
                op.writeTo(dos);
            }
            dos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getNumOperations() {
        return this.operations.length;
    }

    public CoinOperation[] getOperations() {
        return operations;
    }

    @Override
    public void add(int index, short data) {
        throw new UnsupportedOperationException();
    }
}
