package demo.coin.core.operation;

import bftsmart.tom.core.messages.TOMMessage;
import demo.coin.core.transactions.CoinOperation;
import demo.coin.util.ByteArray;
import demo.coin.util.ByteUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static demo.coin.util.ByteUtils.readUnsignedShort;

/**
 * Suporta até 255 operações simultaneas.
 */
public class CoinMultiOperationRequest {

    public final TOMMessage request;
    private final List<ByteArray> operations;


    public CoinMultiOperationRequest() {
        this.request = null;
        this.operations = new ArrayList<>();
    }

    /**
     * O formato do array de multioperações é assim
     * <(ubyte)num_ops>[<(ushort)sizeOp><OP>]
     */
    public CoinMultiOperationRequest(TOMMessage request) {
        this.request = request;
        this.operations = readOperations(request.getContent());
    }

    private List<ByteArray> readOperations(byte[] buffer) {
        int numOp = ByteUtils.readUnsignedByte(buffer, 0);
        var operations = new ArrayList<ByteArray>(numOp);
        int index = 1;
        for (int op = 0; op < numOp; op++) {
            int sizeOp = readUnsignedShort(buffer, index);
            index += 2;
            byte[] operation = new byte[sizeOp];
            operations.add(new ByteArray(operation));
            System.arraycopy(buffer, index, operation, 0, sizeOp);
            index += sizeOp;
        }
        return operations;
    }

    public byte[] serialize() {
        try (var baos = new ByteArrayOutputStream();
             var dos = new DataOutputStream(baos)) {
            dos.writeByte(operations.size());
            for (var op : operations) {
                dos.writeShort(op.length);
                dos.write(op.bytes);
            }
            dos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void add(CoinOperation operation) {
        this.operations.add(new ByteArray(operation.toByteArray()));
    }


    public int getNumOps() {
        return operations.size();
    }


    public byte[] getOp(int i) {
        return operations.get(i).bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CoinMultiOperationRequest that = (CoinMultiOperationRequest) o;
        return operations.equals(that.operations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operations);
    }

    @Override
    public String toString() {
        return "CoinMultiOperationRequest{" +
                "operations=" + operations +
                '}';
    }
}
