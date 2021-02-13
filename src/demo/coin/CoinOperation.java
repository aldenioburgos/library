package demo.coin;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class CoinOperation {

    protected byte[] issuer;
    protected byte[] signature;

    public static final CoinOperation loadFrom(byte[] bytes) {
        try (var bais = new ByteArrayInputStream(bytes);
             var dis = new DataInputStream(bais)) {
            return loadFrom(dis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static final CoinOperation loadFrom(DataInputStream dis) {
        try {
            byte opType = dis.readByte();
            CoinOperation operation = switch (opType) {
                case Mint.OP_TYPE -> new Mint();
                case Transfer.OP_TYPE -> new Transfer();
                default -> throw new IllegalArgumentException("Tipo de operação desconhecido <" + opType + ">.");
            };
            operation.loadFromWorker(dis);
            return operation;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    protected void writeTo(DataOutputStream dos) throws IOException {
        dos.writeByte(getOpType());
        dos.write(issuer);
        dos.write(signature);
    }

    protected void loadFromWorker(DataInputStream dis) throws IOException {
        issuer = dis.readNBytes(32);
        signature = dis.readNBytes(32);
    }

    protected abstract byte getOpType();

    public abstract void execute(CoinGlobalState globalState);
}
