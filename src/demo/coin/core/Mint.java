package demo.coin.core;

import demo.coin.util.CryptoUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Mint extends CoinOperation {

    // data
    private long value;

    protected Mint() {
    }

    public Mint(byte[] issuer, long value, long nonce, byte currency) {
        super(issuer, currency);
        this.value = value;
    }

    @Override
    protected void loadDataFrom(DataInputStream dis) throws IOException {
        super.loadDataFrom(dis);
        value = dis.readLong();
    }

    @Override
    protected void writeDataTo(DataOutputStream dos) throws IOException {
        super.writeDataTo(dos);
        dos.writeLong(value);
    }

    @Override
    protected byte getOpType() {
        return MINT_TYPE;
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
        return super.isInvalid(globalState) || (value <= 0) || !globalState.isMinter(this.issuer);
    }

    @Override
    public String toString() {
        return "Mint{" +
                super.toString() +
                ", value=" + value +
                '}';
    }
}
