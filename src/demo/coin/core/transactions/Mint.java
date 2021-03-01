package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.util.CryptoUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//  /------------header-------------\-/---------body--------\
//   <op_type>[<accounts>]<signature> <currency><value>
//   <1>       <1><91>    <71>        <1>       <8>
// total: 1+1+91+71+1+8 = 173 bytes
public class Mint extends CoinOperation {

    // data
    private byte currency;
    private long value;

    public Mint(byte[] issuer, long value, byte currency) {
        super(issuer);
        this.currency = currency;
        this.value = value;
    }

    public Mint(byte[] bytes) {
        load(bytes);
    }

    @Override
    protected void validateType(DataInputStream dis) throws IOException {
        // validate type
        int type = dis.readUnsignedByte();
        if (type != OP_TYPE.MINT.ordinal()) throw new RuntimeException("Invalid Type! " + type);
    }

    @Override
    protected void loadDataFrom(DataInputStream dis) throws IOException {
        currency = dis.readByte();
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
            if (isInvalid(globalState)) {
                throw new IllegalArgumentException("Operação inválida <" + this + ">.");
            }
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
    public boolean isInvalid(CoinGlobalState globalState) {
        // as saídas tem valor positivo? //o emissor pode cunhar moedas?
        return super.isInvalid(globalState) || (value <= 0) || !globalState.isMinter(accounts.get(this.issuer)) || !globalState.isCurrency(currency);
    }

    @Override
    public String toString() {
        return "Mint{" +
                super.toString() +
                ", currency=" + currency +
                ", value=" + value +
                '}';
    }
}
