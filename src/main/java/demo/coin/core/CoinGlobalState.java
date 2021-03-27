package demo.coin.core;

import demo.coin.util.ByteArray;
import demo.coin.util.ByteUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static demo.coin.core.transactions.CoinOperation.ISSUER_SIZE;

public class CoinGlobalState {

    private final Set<ByteArray> minters;
    private final Map<ByteArray, Set<Utxo>>[] shards;


    public CoinGlobalState(Set<ByteArray> minters, Map<ByteArray, Set<Utxo>>[] shards) {
        this.minters = minters;
        this.shards = shards;
    }

    public CoinGlobalState() {
        this(1);
    }

    public CoinGlobalState(int currencies) {
        this(new HashSet<>(), new HashSet<>(), currencies);
    }

    public CoinGlobalState(Set<ByteArray> minters, Set<ByteArray> users, int currencies) {
        //@formatter:off
        if (currencies <= 0)                            throw new IllegalArgumentException();
        if (minters == null || minters.isEmpty())       throw new IllegalArgumentException();
        if (users == null || users.isEmpty())           throw new IllegalArgumentException();
        //@formatter:on
        this.minters = Set.copyOf(minters);
        this.shards = new Map[currencies];
        initShards(minters, users);
    }

    private void initShards(Set<ByteArray> minters, Set<ByteArray> users) {
        // os mineradores também são usuários
        users.addAll(minters);
        for (int currency = 0; currency < shards.length; currency++) {
            Map<ByteArray, Set<Utxo>> shard = new HashMap<>();
            users.forEach(it -> shard.put(it, new HashSet<>()));
            this.shards[currency] = shard;
        }
    }

    public boolean isMinter(ByteArray minter) {
        //@formatter:off
        if (minter == null || minter.length == 0)       throw new IllegalArgumentException();
        //@formatter:on
        return minters.contains(minter);
    }

    public void addUser(ByteArray user) {
        //@formatter:off
        if (user == null || user.length != ISSUER_SIZE) throw new IllegalArgumentException("Illegal user detected " + user);
        //@formatter:on
        for (int currency = 0; currency < shards.length; currency++) {
            this.shards[currency].put(user, new HashSet<>());
        }
    }


    public boolean isUser(ByteArray user) {
        return this.shards[0].containsKey(user);
    }

    public boolean isCurrency(int currency) {
        return currency < shards.length && currency >= 0;
    }

    public Set<Utxo> getUtxos(int currency, ByteArray owner) {
        //@formatter:off
        if (currency < 0 || currency >= shards.length || shards[currency] == null)    throw new IllegalArgumentException();
        if (owner == null || owner.length != ISSUER_SIZE)                           throw new IllegalArgumentException();
        //@formatter:on
        Set<Utxo> oldUtxos = shards[currency].get(owner);
        if (oldUtxos == null || oldUtxos.isEmpty()) {
            return new HashSet<>();
        } else {
            return Set.copyOf(oldUtxos);
        }
    }

    public Set<Utxo> getUtxos(int currency, ByteArray owner, Set<UtxoAddress> utxoToSearchFor) {
        //@formatter:off
        if (utxoToSearchFor == null || utxoToSearchFor.isEmpty())                   throw new IllegalArgumentException();
        //@formatter:on

        Set<Utxo> oldUtxos = getUtxos(currency, owner);
        return oldUtxos.stream().filter(it -> utxoToSearchFor.contains(it.address)).collect(Collectors.toSet());
    }

    public void removeUtxos(int currency, ByteArray owner, Set<UtxoAddress> addresses) {
        //@formatter:off
        if (currency < 0 || currency >= shards.length || shards[currency] == null)     throw new IllegalArgumentException("Invalid currency " + currency);
        if (owner == null || owner.length != ISSUER_SIZE)                            throw new IllegalArgumentException();
        if (addresses == null)                                                       throw new IllegalArgumentException();
        //@formatter:on

        Set<Utxo> oldUtxos = shards[currency].get(owner);
        shards[currency].put(owner, oldUtxos.stream().filter(it -> !addresses.contains(it.address)).collect(Collectors.toSet()));
    }

    public void addUtxo(int currency, ByteArray owner, byte[] transactionHash, int outputPosition, long value) {
        Set<Utxo> oldUtxos = shards[currency].get(owner);
        oldUtxos.add(new Utxo(transactionHash, outputPosition, value));
    }

    @Override
    public String toString() {
        return "CoinGlobalState{" +
                ", minters=[" + minters.stream().map(it -> it.bytes).map(ByteUtils::convertToText).collect(Collectors.joining(", ")) +
                "], utxos=" + Arrays.toString(shards) +
                '}';
    }
}
