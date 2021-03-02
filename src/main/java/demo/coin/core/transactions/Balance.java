package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.core.Utxo;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

//  /-------------header-------------------\-/---------body--------\
//   <op_type>[    <accounts>   ]<signature>  [<currencies>]
//   <1>       <1>[<91>..<23205>]    <71>      <1>[<1>..<255>]
// total: 1+1+(91..23205)+71                   +1+(1..255)           = 166..23534 bytes
public class Balance extends CoinOperation {

    private byte[] currencies;

    public Balance(byte[] bytes) {
        load(bytes);
    }

    public Balance(byte[] issuer, byte[] currencies) {
        super(issuer);
        this.currencies = currencies;
    }

    @Override
    protected void validateType(DataInputStream dis) throws IOException {
        int type = dis.readUnsignedByte();
        if (type != OP_TYPE.BALANCE.ordinal()) throw new RuntimeException("Invalid Type! " + type);
    }

    @Override
    protected OP_TYPE getOpType() {
        return OP_TYPE.BALANCE;
    }

    @Override

    public byte[] execute(CoinGlobalState globalState) {
        try (var baos = new ByteArrayOutputStream(2 + (currencies.length * 9));
             var dos = new DataOutputStream(baos)) {
            Map<Byte, Long> balances = new HashMap<>(currencies.length);
            for (byte currency : currencies) {
                long balance = globalState.listUtxos(accounts.get(issuer), currency).stream()
                        .map(Utxo::getValue)
                        .reduce(0L, Long::sum);
                balances.put(currency, balance);
            }
            dos.writeBoolean(true);
            dos.writeByte(balances.size());
            for (byte currency : currencies) {
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
        currencies = dis.readNBytes(length);
    }

    @Override
    protected void writeDataTo(DataOutputStream dos) throws IOException {
        dos.writeByte(currencies.length);
        dos.write(currencies);
    }
}
