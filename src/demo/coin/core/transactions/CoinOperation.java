package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.util.CryptoUtil;

import java.io.*;
import java.util.Arrays;

import static demo.coin.util.CryptoUtil.checkSignature;

public abstract class CoinOperation {

    public enum OP_TYPE {MINT, TRANSFER, EXCHANGE, BALANCE}

    public static final int ISSUER_SIZE = 91;
    public static final int HASH_SIZE = 32;
    public static final int SIGNATURE_SIZE = 71;

    // header
    // <op_type><issuer><signature>
    // <1>      <91>    <71>
    protected byte[] issuer;
    protected byte[] signature;

    protected CoinOperation() {
    }

    public CoinOperation(byte[] issuer) {
        this.issuer = issuer;
    }

    public void sign(byte[] privateKeyBytes) {
        this.signature = CryptoUtil.sign(privateKeyBytes, getDataBytes());
    }

    public static CoinOperation loadFrom(byte[] bytes) {
        try (var bais = new ByteArrayInputStream(bytes);
             var dis = new DataInputStream(bais)) {
            return loadFrom(dis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static CoinOperation loadFrom(DataInputStream dis) {
        try {
            OP_TYPE opType = OP_TYPE.values()[dis.readByte()];
            CoinOperation operation = switch (opType) {
                case MINT -> new Mint();
                case TRANSFER -> new Transfer();
                case EXCHANGE -> new Exchange();
                case BALANCE -> new Balance();
                default -> throw new IllegalArgumentException("Tipo de operação desconhecido <" + opType + ">.");
            };
            operation.issuer = dis.readNBytes(ISSUER_SIZE);
            operation.signature = dis.readNBytes(SIGNATURE_SIZE);
            operation.loadDataFrom(dis);
            return operation;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeTo(DataOutputStream dos) throws IOException {
        dos.writeByte(getOpType().ordinal());
        dos.write(issuer);
        dos.write(signature);
        writeDataTo(dos);
    }

    public boolean isInvalid(CoinGlobalState globalState) {
        // os arrays tem o tamanho certo?
        return (issuer == null || issuer.length != ISSUER_SIZE || !globalState.isUser(issuer)) ||
               (signature == null || signature.length != SIGNATURE_SIZE || !isSignatureValid());
    }

    protected boolean isSignatureValid() {
        return checkSignature(issuer, getDataBytes(), signature);
    }

    protected byte[] toByteArray() {
        try (var baos = new ByteArrayOutputStream();
             var dos = new DataOutputStream(baos)) {
            this.writeTo(dos);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected byte[] getDataBytes() {
        try (var baos = new ByteArrayOutputStream();
             var dos = new DataOutputStream(baos)) {
            this.writeDataTo(dos);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "issuer=" + Arrays.toString(issuer) +
                ", signature=" + Arrays.toString(signature);

    }

    protected abstract OP_TYPE getOpType();

    public abstract void execute(CoinGlobalState globalState);

    protected abstract void loadDataFrom(DataInputStream dis) throws IOException;

    protected abstract void writeDataTo(DataOutputStream dos) throws IOException;

}
