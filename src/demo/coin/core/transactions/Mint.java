package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.util.CryptoUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Mint extends CoinOperation {

    // data
    private byte currency;
    private long value;

    protected Mint() {
    }

    public Mint(byte[] issuer, long value, byte currency) {
        super(issuer);
        this.currency = currency;
        this.value = value;
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
    public void execute(CoinGlobalState globalState) {
        if (isInvalid(globalState)) {
            throw new IllegalArgumentException("Operação inválida <" + this + ">.");
        }
        // atualiza o global state: criar os utxos de saída para o próprio emissor.
        globalState.addUtxo(issuer, CryptoUtil.hash(toByteArray()), (byte) 0, value, currency);
    }


    @Override
    public boolean isInvalid(CoinGlobalState globalState) {
        // as saídas tem valor positivo? //o emissor pode cunhar moedas?
        return super.isInvalid(globalState) || (value <= 0) || !globalState.isMinter(this.issuer) || !globalState.isCurrency(currency);
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
