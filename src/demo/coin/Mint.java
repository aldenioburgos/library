package demo.coin;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Mint extends CoinOperation {

    public static final byte OP_TYPE = 0;
    private long value;
    private long nonce;

    public Mint() {
    }

    public Mint(byte[] issuer, byte[] signature, long value, long nonce) {
        super(issuer, signature);
        this.value = value;
        this.nonce = nonce;
    }

    @Override
    protected void loadFromWorker(DataInputStream dis) throws IOException {
        super.loadFromWorker(dis);
        value = dis.readLong();
        nonce = dis.readLong();
    }

    @Override
    protected void writeTo(DataOutputStream dos) throws IOException {
        super.writeTo(dos);
        writeDataTo(dos);
    }

    private void writeDataTo(DataOutputStream dos) throws IOException {
        dos.writeLong(value);
        dos.writeLong(nonce);
    }

    @Override
    protected byte getOpType() {
        return OP_TYPE;
    }

    @Override
    public void execute(CoinGlobalState globalState) {
        if (!isValid(globalState)) {
            throw new IllegalArgumentException("Operação inválida <" + this + ">.");
        }
        // atualiza o global state: criar os utxos de saída para o próprio emissor.
        globalState.addUtxo(issuer, getTransactionHash(), (byte) 0, value);
    }


    @Override
    public byte[] getDataBytes() {
        try(var baos = new ByteArrayOutputStream();
            var dos = new DataOutputStream(baos)){
            this.writeDataTo(dos);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isValid(CoinGlobalState globalState) {
       // as saídas tem valor positivo? e o emissor pode cunhar moedas?
        return super.isValid(globalState) && (value > 0) && globalState.isMinter(this.issuer);
    }

    @Override
    public String toString() {
        return "Mint{" +
                super.toString() +
                ", value=" + value +
                '}';
    }
}
