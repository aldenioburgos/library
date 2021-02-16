package demo.coin;

import java.util.Objects;

public class Utxo {
    private final UtxoAddress address;
    private final long value;

    public Utxo(byte[] transactionHash, byte outputPosition) {
        this(new UtxoAddress(transactionHash, outputPosition));
    }
    public Utxo(byte[] transactionHash, byte outputPosition, long value) {
        this(new UtxoAddress(transactionHash, outputPosition), value);
    }

    public Utxo(UtxoAddress address) {
        this(address, 0);
    }

    public Utxo(UtxoAddress address, long value) {
        this.address = address;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Utxo utxo = (Utxo) o;
        return Objects.equals(address, utxo.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    public long getValue() {
        return value;
    }

    public Byte getOutputPosition() {
        return address.getOutputPosition();
    }

    public byte[] getTransactionHash() {
        return address.getTransactionHash();
    }
}
