package demo.coin.core;

import java.util.Objects;

public class Utxo {
    private final UtxoAddress address;
    private final long value;

    public Utxo(byte[] transactionHash, int outputPosition, long value) {
        this(new UtxoAddress(transactionHash, outputPosition), value);
    }

    public Utxo(byte[] transactionHash, int outputPosition) {
        this(new UtxoAddress(transactionHash, outputPosition), 0);
    }

    public Utxo(UtxoAddress address) {
        this(address, 0);
    }

    public Utxo(UtxoAddress address, long value) {
        this.address = address;
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    public int getOutputPosition() {
        return address.getOutputPosition();
    }

    public byte[] getTransactionHash() {
        return address.getTransactionHash();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Utxo))
            return false;
        Utxo utxo = (Utxo) o;
        return getValue() == utxo.getValue() && Objects.equals(address, utxo.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, getValue());
    }

    @Override
    public String toString() {
        return "Utxo{" +
                "address=" + address +
                ", value=" + value +
                '}';
    }
}
