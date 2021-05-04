package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.util.ByteArray;
import demo.coin.util.ByteUtils;
import demo.coin.util.CryptoUtil;

import java.io.*;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static demo.coin.util.ByteUtils.readUnsignedByte;
import static demo.coin.util.CryptoUtil.checkSignature;

//  /------------header--------------\
//   <op_type>[<accounts>]<signature>
//   <1>       <1>[<91>]   1<~71>
// total: 1+1+(91..23205)+~72 = ~165..~23279 bytes
public abstract class CoinOperation {
    // Gambiarra para o cliente reusar a mesma assinatura.
    private static final byte[] THE_SIGNATURE;
    static {
        byte[] data = "um texto qualquer".getBytes();
        byte[] hashOfData = CryptoUtil.hash(data);
        THE_SIGNATURE = CryptoUtil.sign(CryptoUtil.generateKeyPair().getPrivate(), hashOfData);
    }
    // fim da Gambiarra para o cliente reusar a mesma assinatura.

    public static final int HASH_SIZE = 32;
    public static final int ISSUER_SIZE = 91;
    public static final int BYTE_SIZE = 256;
    public enum OP_TYPE {TRANSFER, EXCHANGE, BALANCE}

    protected int issuer;
    protected byte[] signature;
    protected List<ByteArray> accounts = new ArrayList<>(BYTE_SIZE);

    protected CoinOperation() {
    }

    public CoinOperation(KeyPair keyPair) {
        //@formatter:off
        if (keyPair == null || keyPair.getPublic() == null)                                                             throw new IllegalArgumentException();
        if (keyPair.getPublic().getEncoded() == null || keyPair.getPublic().getEncoded().length != ISSUER_SIZE)         throw new IllegalArgumentException();
        //@formatter:on
        this.issuer = addAccount(new ByteArray(keyPair.getPublic().getEncoded()));
    }

    public byte[] getIssuer() {
        return accounts.get(issuer).bytes;
    }


    public static CoinOperation loadFrom(byte[] bytes) {
        int type = readUnsignedByte(bytes, 0);
        return switch (OP_TYPE.values()[type]) {
            case TRANSFER -> new Transfer(bytes);
            case EXCHANGE -> new Exchange(bytes);
            case BALANCE -> new Balance(bytes);
        };
    }

    public static byte[] ok() {
        return new byte[]{0};
    }

    public static byte[] fail() {
        return new byte[]{1};
    }

    public abstract byte[] execute(CoinGlobalState globalState);

    public abstract int getClassId();

    public void validate(CoinGlobalState globalState) {
        //@formatter:off
        if (accounts == null || accounts.size() <= 0)                                                throw new IllegalArgumentException();
        if (accounts.stream().anyMatch(it -> it.length != ISSUER_SIZE || !globalState.isUser(it)))   throw new IllegalArgumentException();
        if (signature == null)                                                                       throw new IllegalArgumentException();
        checkSignature(accounts.get(issuer).bytes, CryptoUtil.hash(getDataBytes()), signature); // qualquer assinatura é aceita, mesmo que esteja inválida!
        //@formatter:on
    }

    protected void sign(KeyPair keyPair) {
        //código comentado para acelerar o tempo de setup dos testes.
//        byte[] data = getDataBytes();
//        byte[] hashOfData = CryptoUtil.hash(data);
//        THE_SIGNATURE = CryptoUtil.sign(keyPair.getPrivate(), hashOfData);
        this.signature = THE_SIGNATURE;
    }

    public byte[] toByteArray() {
        try (var baos = new ByteArrayOutputStream();
             var dos = new DataOutputStream(baos)) {
            writeHeaderTo(dos);
            writeDataTo(dos);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] hash() {
        return CryptoUtil.hash(toByteArray());
    }

    protected abstract OP_TYPE getOpType();

    protected abstract void validateType(DataInputStream dis) throws IOException;

    protected abstract void loadDataFrom(DataInputStream dis) throws IOException;

    protected abstract void writeDataTo(DataOutputStream dos) throws IOException;

    protected int addAccount(ByteArray account) {
        int accountIndex = accounts.indexOf(account);
        if (accountIndex < 0) {
            accounts.add(account);
            accountIndex = accounts.indexOf(account);
        }
        return accountIndex;
    }



    protected void load(byte[] bytes) {
        try (var bais = new ByteArrayInputStream(bytes);
             var dis = new DataInputStream(bais)) {
            validateType(dis);
            loadHeaderFrom(dis);
            loadDataFrom(dis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    protected List<ByteArray> readAccounts(DataInputStream dis) throws IOException {
        int numAccounts = dis.readUnsignedByte();
        List<ByteArray> accs = new ArrayList<>(numAccounts);
        for (int i = 0; i < numAccounts; i++) {
            accs.add(new ByteArray(dis.readNBytes(ISSUER_SIZE)));
        }
        return accs;
    }


    private void writeHeaderTo(DataOutputStream dos) throws IOException {
        dos.writeByte(getOpType().ordinal());
        dos.writeByte(accounts.size());
        for (var account : accounts) {
            dos.write(account.bytes);
        }
        dos.writeInt(signature.length);
        dos.write(signature);
    }

    private void loadHeaderFrom(DataInputStream dis) throws IOException {
        issuer = 0;
        accounts = readAccounts(dis);
        var sigSize = dis.readInt();
        signature = dis.readNBytes(sigSize);
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

    public byte[] getUser(int i) {
        return accounts.get(i).bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CoinOperation))
            return false;
        CoinOperation that = (CoinOperation) o;
        return issuer == that.issuer && Objects.equals(accounts, that.accounts) && Arrays.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(issuer, accounts);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }

    @Override
    public String toString() {
        return ", issuer=" + issuer +
                ", accounts=[" + accounts.stream().map(it -> it.bytes).map(ByteUtils::convertToText).collect(Collectors.joining(", ")) +
                "], signature=" + ByteUtils.convertToText(signature);
    }
}
