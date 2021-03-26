package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.util.ByteArray;
import demo.coin.util.ByteUtils;
import demo.coin.util.CryptoUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.util.*;

import static demo.coin.core.transactions.CoinOperation.OP_TYPE.REGISTER_USERS;
import static demo.coin.util.CryptoUtil.checkSignature;

public class RegisterUsers extends CoinOperation {

    private List<Integer> users;

    public RegisterUsers(byte[] bytes) {
        load(bytes);
    }

    public RegisterUsers(KeyPair keyPair, Set<KeyPair> usersToRegister) {
        super(keyPair);
        //@formatter:off
        if (usersToRegister == null || usersToRegister.isEmpty() || usersToRegister.size() > 256) throw new IllegalArgumentException();
        //@formatter:on
        this.users = new ArrayList<>(usersToRegister.size());
        for (var user : usersToRegister) {
            int userPos = addAccount(new ByteArray(user.getPublic().getEncoded()));
            this.users.add(userPos);
        }
        sign(keyPair);
    }

    @Override
    public byte[] execute(CoinGlobalState globalState) {
        try {
            // a transação é valida?
            validate(globalState);
            // adiciona os usuários
            for (var user : this.users) {
                globalState.addUser(accounts.get(user));
            }
            return CoinOperation.ok();
        } catch (Throwable e) {
            e.printStackTrace();
            return CoinOperation.fail();
        }
    }

    @Override
    public void validate(CoinGlobalState globalState) {
        //@formatter:off
        if (accounts == null || accounts.size() <= 0)                                                   throw new IllegalArgumentException();
        if (!globalState.isMinter(accounts.get(issuer)))                                                throw new IllegalArgumentException();
        if (signature == null)                                                                          throw new IllegalArgumentException();
        if (!checkSignature(accounts.get(issuer).bytes, CryptoUtil.hash(getDataBytes()), signature))    throw new IllegalArgumentException();
        //@formatter:on
    }

    @Override
    public int getClassId(Set<SortedSet<Integer>> allPossiblePartitionsArrangement) {
        SortedSet<Integer> biggestSet = allPossiblePartitionsArrangement.stream().reduce(new TreeSet<>(), (a, b) -> (a.size() > b.size()) ? a : b);
        return biggestSet.toString().hashCode();
    }

    @Override
    protected OP_TYPE getOpType() {
        return REGISTER_USERS;
    }

    @Override
    protected void validateType(DataInputStream dis) throws IOException {
        //@formatter:off
        if (dis.readUnsignedByte() != REGISTER_USERS.ordinal()) throw new IllegalArgumentException("Invalid Type! ");
        //@formatter:on
    }

    @Override
    protected void loadDataFrom(DataInputStream dis) throws IOException {
        int size = dis.readUnsignedByte();
        this.users = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            users.add(dis.readUnsignedByte());
        }
    }

    @Override
    protected void writeDataTo(DataOutputStream dos) throws IOException {
        dos.writeByte(users.size());
        for (var user : users) {
            dos.writeByte(user);
        }
    }

    @Override
    public String toString() {
        return "RegisterUsers{ " +
                "#newUsers=" + (accounts.size() - 1) +
                super.toString() +
                "}";
    }
}
