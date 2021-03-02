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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UtxoAddress that = (UtxoAddress) o;
        return outputPosition == that.outputPosition && Arrays.equals(transactionHash, that.transactionHash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(outputPosition);
        result = 31 * result + Arrays.hashCode(transactionHash);
        return result;
    }

    public byte[] getTransactionHash() {
        return transactionHash;
    }

    public int getOutputPosition() {
        return outputPosition;
    }
}
