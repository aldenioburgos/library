package demo.coin.core;

import java.util.Arrays;
import java.util.Objects;

public class UtxoAddress {
    private final byte[] transactionHash;
    private final int outputPosition; // Esse campo é um unsignedbyte, usei integer pq não temos restrição de espaço

    public UtxoAddress(byte[] transactionHash, int outputPosition) {
        this.transactionHash = transactionHash;
        this.outputPosition = outputPosition;
    }

    public byte[] getTransactionHash() {
        return transactionHash;
    }

    public int getOutputPosition() {
        return outputPosition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof UtxoAddress))
            return false;
        UtxoAddress that = (UtxoAddress) o;
        return getOutputPosition() == that.getOutputPosition() && Arrays.equals(getTransactionHash(), that.getTransactionHash());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getOutputPosition());
        result = 31 * result + Arrays.hashCode(getTransactionHash());
        return result;
    }

    @Override
    public String toString() {
        return "{" +
                "transactionHash=" + Arrays.toString(transactionHash) +
                ", outputPosition=" + outputPosition +
                '}';
    }
}
