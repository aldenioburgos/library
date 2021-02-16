package demo.coin;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.*;

public class CoinGlobalState {

    private final Map<byte[], Set<Long>> minters = new HashMap<>();
    private final Map<byte[], Set<Utxo>> utxos = new HashMap<>();


    public boolean isMinter(byte[] minter) {
        synchronized (minters) {
            return minters.containsKey(minter);
        }
    }

    public void addMinter(byte[] minter) {
        synchronized (minters) {
            if (!minters.containsKey(minter)) minters.put(minter, new HashSet<>());
        }
    }

    public Set<Utxo> getUtxos(byte[] owner) {
        Set<Utxo> oldUtxos = this.utxos.get(owner);
        if (oldUtxos == null) {
            //noinspection unchecked
            return EMPTY_SET;
        } else {
            return unmodifiableSet(oldUtxos);
        }
    }

    public void removeUtxos(byte[] owner, Set<UtxoAddress> addresses) {
        Set<Utxo> oldUtxos;
        synchronized (this.utxos) {
            oldUtxos = utxos.get(owner);
        }
        if (oldUtxos != null) {
            Set<Utxo> utxosToRemove = addresses.stream().map(Utxo::new).collect(Collectors.toSet());
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (oldUtxos) {
                oldUtxos.removeAll(utxosToRemove);
            }
        }
    }


    public void addUtxo(byte[] owner, byte[] transactionHash, byte outputPosition, long value) {
        var newUtxo = new Utxo(transactionHash, outputPosition, value);
        Set<Utxo> oldUtxos = this.utxos.get(owner);
        if (oldUtxos == null) {
            synchronized (this.utxos) {
                Set<Utxo> newUtxos = new HashSet<>();
                newUtxos.add(newUtxo);
                this.utxos.put(owner, newUtxos);
            }
        } else {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (oldUtxos) {
                oldUtxos.add(newUtxo);
            }
        }
    }


}
