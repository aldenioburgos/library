package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.util.CryptoUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static demo.coin.util.ByteUtils.readUnsignedByte;
import static demo.coin.util.CryptoUtil.checkSignature;

//  /------------header--------------\
//   <op_type>[<accounts>]<signature>
//   <1>       <1>[<91>]  <71>
// total: 1+1+(91..23205)+71 = 164..23278 bytes
public abstract class CoinOperation {

    public enum OP_TYPE {MINT, TRANSFER, EXCHANGE, BALANCE}

    public static final int ISSUER_SIZE = 91;
    public static final int HASH_SIZE = 32;
    public static final int SIGNATURE_SIZE = 71;

    protected byte issuer;
    protected List<byte[]> accounts;
    protected byte[] signature;

    protected CoinOperation() {
    }

    public CoinOperation(byte[] issuer) {
        this.issuer = 0;
        this.accounts = new ArrayList<>();
        this.accounts.add(issuer);
    }


    public static CoinOperation loadFrom(byte[] bytes) {
        int type = readUnsignedByte(bytes, 0);
        return switch (OP_TYPE.values()[type]) {
            case MINT -> new Mint(bytes);
            case TRANSFER -> new Transfer(bytes);
            case EXCHANGE -> new Exchange(bytes);
            case BALANCE -> new Balance(bytes);
        };
    }


    protected List<byte[]> readAccounts(DataInputStream dis) throws IOException {
        int numAccounts = dis.readUnsignedByte();
        List<byte[]> accs = new ArrayList<>(numAccounts);
        for (int i = 0; i < numAccounts; i++) {
            accs.add(dis.readNBytes(ISSUER_SIZE));
        }
        return accs;
    }

    public void sign(byte[] privateKeyBytes) {
        this.signature = CryptoUtil.sign(privateKeyBytes, getDataBytes());
    }

    public void writeTo(DataOutputStream dos) throws IOException {
        dos.writeByte(getOpType().ordinal());
        dos.writeByte(accounts.size());
        for (var account : accounts) {
            dos.write(account);
        }
        dos.write(signature);
        writeDataTo(dos);
    }

    public boolean isInvalid(CoinGlobalState globalState) {
        var accValid = accounts != null && accounts.size() >= 1 && accounts.stream().allMatch(it -> it.length == ISSUER_SIZE && globalState.isUser(it));
        return accValid && signature != null && signature.length == SIGNATURE_SIZE && checkSignature(accounts.get(issuer), getDataBytes(), signature);
    }


    protected void loadHeaderFrom(DataInputStream dis) throws IOException {
        issuer = 0;
        accounts = readAccounts(dis);
        signature = dis.readNBytes(SIGNATURE_SIZE);
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
        return "accounts=" + accounts +
                ", issuer=" + issuer +
                ", signature=" + Arrays.toString(signature);

    }

    protected abstract OP_TYPE getOpType();

    public abstract byte[] execute(CoinGlobalState globalState);

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

    protected abstract void validateType(DataInputStream dis) throws IOException;

    protected abstract void loadDataFrom(DataInputStream dis) throws IOException;

    protected abstract void writeDataTo(DataOutputStream dos) throws IOException;


    protected byte[] ok() {
        return new byte[]{0};
    }
    protected byte[] fail() {
        return new byte[]{1};
    }
}
