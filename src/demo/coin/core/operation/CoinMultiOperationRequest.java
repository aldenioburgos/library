package demo.coin.core.operation;

import bftsmart.tom.core.messages.TOMMessage;

import static demo.coin.util.ByteUtils.readUnsignedShort;

public class CoinMultiOperationRequest {

    public final TOMMessage request;
    private byte[][] operations;

    /**
     * O formato do array de multioperações é assim
     * <(ubyte)num_ops>[<(ushort)sizeOp><OP>]
     */
    public CoinMultiOperationRequest(TOMMessage request) {
        this.request = request;
        this.operations = readOperations(request.getContent());
    }

    private byte[][] readOperations(byte[] buffer) {
        int numOp = buffer[0] & 0xff;
        var operations = new byte[numOp][];
        int index = 1;
        for (int op = 0; op < numOp; op++) {
            int sizeOp = readUnsignedShort(buffer, index);
            byte[] operation = new byte[sizeOp];
            System.arraycopy(buffer, index, operation, 0, sizeOp);
            operations[op] = operation;
            index += sizeOp + 2;
        }
        return operations;
    }


    public int getNumOps() {
        return operations.length;
    }


    public byte[] getOp(int i) {
        return operations[i];
    }
}
