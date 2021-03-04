package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.core.Utxo;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static demo.coin.util.ByteUtils.byteArrayToIntArray;
import static demo.coin.util.ByteUtils.intArrayToByteArray;

//  /-------------header-------------------\-/---------body--------\
//   <op_type>[    <accounts>   ]<signature>  [<currencies>]
//   <1>       <1>[<91>..<23205>]    <71>      <1>[<1>..<256>]
// total: 1+1+(91..23205)+71                   +1+(1..256)           = 166..23534 bytes
public class Balance extends CoinOperation {

    private int[] currencies;

    public Balance(byte[] bytes) {
        load(bytes);
    }

    public Balance(KeyPair keyPair, int... currencies) {
        super(keyPair);
        if (Arrays.stream(currencies).anyMatch(it -> it < 0 || it > 255)) throw new IllegalArgumentException();
        this.currencies = currencies;
        sign(keyPair.getPrivate().getEncoded());
    }

    @Override
    protected void validateType(DataInputStream dis) throws IOException {
        int type = dis.readUnsignedByte();
        if (type != OP_TYPE.BALANCE.ordinal())
            throw new RuntimeException("Invalid Type! " + type);
    }

    @Override
    protected OP_TYPE getOpType() {
        return OP_TYPE.BALANCE;
    }

    @Override

    public byte[] execute(CoinGlobalState globalState) {
        try (var baos = new ByteArrayOutputStream(2 + (currencies.length * 9));
             var dos = new DataOutputStream(baos)) {
            Map<Integer, Long> balances = new HashMap<>(currencies.length);
            for (int currency : currencies) {
                long balance = globalState.getUtxos(currency, accounts.get(issuer)).stream()
                        .map(Utxo::getValue)
                        .reduce(0L, Long::sum);
                balances.put(currency, balance);
            }
            dos.writeBoolean(true);
            dos.writeByte(balances.size());
            for (int currency : currencies) {
                dos.writeByte(currency);
                dos.writeLong(balances.get(currency));
            }
            dos.flush();
            return baos.toByteArray();
        } catch (Throwable e) {
            e.printStackTrace();
            return fail();
        }
    }

    @Override
    protected void loadDataFrom(DataInputStream dis) throws IOException {
        int length = dis.readUnsignedByte();
        this.currencies = byteArrayToIntArray(dis.readNBytes(length));
    }

    @Override
    protected void writeDataTo(DataOutputStream dos) throws IOException {
        dos.writeByte(currencies.length);
        dos.write(intArrayToByteArray(currencies));
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Balance))
            return false;
        if (!super.equals(o))
            return false;
        Balance balance = (Balance) o;
        return Arrays.equals(currencies, balance.currencies);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(currencies);
        return result;
    }

    @Override
    public String toString() {
        return "Balance{" +
                super.toString() +
                ", currencies=" + Arrays.toString(currencies) +
                '}';
    }
}
