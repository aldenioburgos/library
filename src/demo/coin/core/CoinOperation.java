package demo.coin.core;

import demo.coin.util.CryptoUtil;

import java.io.*;
import java.util.Arrays;

import static demo.coin.util.CryptoUtil.checkSignature;

public abstract class CoinOperation {

    public static final byte MINT_TYPE = 0;
    public static final byte TRANSFER_TYPE = 1;
    public static final byte EXCHANGE_TYPE = 2;

    public static final int ISSUER_SIZE = 91;
    public static final int HASH_SIZE = 32;
    public static final int SIGNATURE_SIZE = 71;

    // header
    protected byte[] issuer;
    protected byte[] signature;
    protected byte currency;

    protected CoinOperation() {
    }

    public CoinOperation(byte[] issuer, byte currency) {
        this.issuer = issuer;
        this.currency = currency;
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
            byte opType = dis.readByte();
            CoinOperation operation = switch (opType) {
                case MINT_TYPE ->  new Mint();
                case TRANSFER_TYPE -> new Transfer();
                case EXCHANGE_TYPE -> new Exchange();
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

    protected void loadDataFrom(DataInputStream dis) throws IOException{
        currency = dis.readByte();
    }

    public void writeTo(DataOutputStream dos) throws IOException {
        dos.writeByte(getOpType());
        dos.write(issuer);
        dos.write(signature);
        writeDataTo(dos);
    }

    public boolean isInvalid(CoinGlobalState globalState) {
        // os arrays tem o tamanho certo?
        return (issuer == null || issuer.length != ISSUER_SIZE) || (signature == null || signature.length != SIGNATURE_SIZE) || !isSignatureValid();
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

    protected void writeDataTo(DataOutputStream dos) throws IOException{
        dos.writeByte(currency);
    }

    @Override
    public String toString() {
        return "issuer=" + Arrays.toString(issuer) +
                ", signature=" + Arrays.toString(signature);
    }


    protected abstract byte getOpType();

    public abstract void execute(CoinGlobalState globalState);

}
