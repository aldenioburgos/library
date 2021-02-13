package demo.coin;

import bftsmart.util.MultiOperationRequest;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CoinMultiOperationRequest extends MultiOperationRequest {

    private List<CoinOperation> operations;

    public CoinMultiOperationRequest(byte[] buffer) {
        super(0, (short) 0); //TODO marreta pra funcionar com o codigo do prof.

        try (ByteArrayInputStream in = new ByteArrayInputStream(buffer);
             DataInputStream dis = new DataInputStream(in)) {
            int numOperations = dis.readInt();
            operations = new ArrayList<>(numOperations);
            for (int i = 0; i < numOperations; i++) {
                operations.add(CoinOperation.loadFrom(dis));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public byte[] serialize() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(operations.size());
            for (var op : operations) {
                op.writeTo(dos);
            }
            dos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void add(CoinOperation operation) {
        this.operations.add(operation);
    }

    @Override
    public int getNumOperations() {
        return this.operations.size();
    }

    @Override
    public void add(int index, short data) {
        throw new UnsupportedOperationException();
    }
}
