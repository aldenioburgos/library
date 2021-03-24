package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.util.ByteArray;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.util.*;

import static demo.coin.core.transactions.CoinOperation.OP_TYPE.REGISTER_USERS;

public class RegisterUsers extends CoinOperation {

    public RegisterUsers(byte[] bytes) {
        load(bytes);
    }

    public RegisterUsers(KeyPair keyPair, Set<KeyPair> users) {
        super(keyPair);
        //@formatter:off
        if (users == null || users.isEmpty() || users.size() > 256) throw new IllegalArgumentException();
        //@formatter:on
        for (var user : users) {
            addAccount(new ByteArray(user.getPublic().getEncoded()));
        }
        sign(keyPair.getPrivate().getEncoded());
    }

    @Override
    public byte[] execute(CoinGlobalState globalState) {
        try {
            for (var account : accounts) {
                globalState.addUser(account);
            }
            return CoinOperation.ok();
        } catch (Throwable e) {
            e.printStackTrace();
            return CoinOperation.fail();
        }
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
    protected void loadDataFrom(DataInputStream dis) {
    }

    @Override
    protected void writeDataTo(DataOutputStream dos) {
    }
}
