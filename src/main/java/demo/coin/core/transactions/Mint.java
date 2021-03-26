package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.util.CryptoUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;

//  /------------header-------------\-/---------body--------\
//   <op_type>[<accounts>]<signature>  <currency><value>
//   <1>       <1><91>    <71>         <1>       <8>
// total: 1+1+91+71+1+8 = 173 bytes
public class Mint extends CoinOperation {

    // data
    private int currency;
    private long value;

    public Mint(byte[] bytes) {
        load(bytes);
    }

    public Mint(KeyPair keypair, int currency, long value) {
        super(keypair);
        if (currency < 0 || currency > 255) throw new IllegalArgumentException();
        if (value <= 0) throw new IllegalArgumentException();
        this.currency = currency;
        this.value = value;
        sign(keypair);
    }

    @Override
    protected void validateType(DataInputStream dis) throws IOException {
        if (dis.readUnsignedByte() != OP_TYPE.MINT.ordinal()) throw new IllegalArgumentException("Invalid Type! ");
    }

    @Override
    protected void loadDataFrom(DataInputStream dis) throws IOException {
        currency = dis.readUnsignedByte();
        value = dis.readLong();
    }

    @Override
    protected void writeDataTo(DataOutputStream dos) throws IOException {
        dos.writeByte(currency);
        dos.writeLong(value);
    }

    @Override
    protected OP_TYPE getOpType() {
        return OP_TYPE.MINT;
    }

    @Override
    public byte[] execute(CoinGlobalState globalState) {
        try {
            validate(globalState);
            // atualiza o global state: criar os utxos de saída para o próprio emissor.
            byte[] transactionHash = CryptoUtil.hash(toByteArray());
            globalState.addUtxo(currency, accounts.get(this.issuer), transactionHash, 0, value);
        } catch (Throwable e) {
            e.printStackTrace();
            return fail();
        }
        return ok();
    }

    @Override
    public int getClassId(Set<SortedSet<Integer>> allPossiblePartitionsArrangement) {
        return ("["+currency+"]").hashCode();
    }


    @Override
    public void validate(CoinGlobalState globalState) {
        super.validate(globalState);
        //@formatter:off
        if (value <= 0)                                                             throw new IllegalArgumentException();
        if (!globalState.isMinter(accounts.get(this.issuer)))                       throw new IllegalArgumentException();
        if (currency < 0 || currency > 255 || !globalState.isCurrency(currency))    throw new IllegalArgumentException();
        //@formatter:on
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Mint))
            return false;
        if (!super.equals(o))
            return false;
        Mint mint = (Mint) o;
        return currency == mint.currency && value == mint.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), currency, value);
    }

    @Override
    public String toString() {
        return "Mint{" +
                "currency=" + currency +
                ", value=" + value +
                super.toString() +
                '}';
    }

    public int getCurrency() {
        return currency;
    }

    public long getValue() {
        return value;
    }
}
