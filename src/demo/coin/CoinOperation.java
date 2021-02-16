package demo.coin;

import demo.coin.util.CryptoUtil;

import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public abstract class CoinOperation {

    public static final int ISSUER_SIZE = 91;
    public static final int SIGNATURE_SIZE = 32;

    protected byte[] issuer;
    protected byte[] signature;

    public CoinOperation() {
    }

    public CoinOperation(byte[] issuer, byte[] signature) {
        this.issuer = issuer;
        this.signature = signature;
    }

    public boolean isSignatureValid() {
        try {
            Signature signerEngine = Signature.getInstance("SHA256withECDSA");
            PublicKey publicKey = CryptoUtil.loadPublicKey(issuer);
            signerEngine.initVerify(publicKey);
            signerEngine.update(getDataBytes());
            return signerEngine.verify(signature);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
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

    protected void loadFromWorker(DataInputStream dis) throws IOException {
        issuer = dis.readNBytes(ISSUER_SIZE);
        signature = dis.readNBytes(SIGNATURE_SIZE);
    }

    protected void writeTo(DataOutputStream dos) throws IOException {
        dos.writeByte(getOpType());
        dos.write(issuer);
        dos.write(signature);
    }

    protected byte[] toByteArray() {
        try (var baos = new ByteArrayOutputStream();
             var dos = new DataOutputStream(baos)) {
            writeTo(dos);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isValid(CoinGlobalState globalState) {
        // os arrays tem o tamanho certo?
        return (issuer != null && issuer.length == ISSUER_SIZE) && (signature != null && signature.length == SIGNATURE_SIZE) && isSignatureValid();
    }

    public byte[] getTransactionHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(this.toByteArray());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "issuer=" + Arrays.toString(issuer) +
                ", signature=" + Arrays.toString(signature);
    }

    protected abstract byte getOpType();

    public abstract void execute(CoinGlobalState globalState);

    public abstract byte[] getDataBytes();

}
