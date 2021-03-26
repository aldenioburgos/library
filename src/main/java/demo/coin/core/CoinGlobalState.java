package demo.coin.core;

import demo.coin.util.ByteArray;
import demo.coin.util.ByteUtils;

import java.util.*;
import java.util.stream.Collectors;

import static demo.coin.core.transactions.CoinOperation.ISSUER_SIZE;

public class CoinGlobalState {

    private final int[]                       currencies;  // 0 .. 255 (byte sized currency)
    private final Set<ByteArray>              minters;
    private final Set<ByteArray>              users;
    private final Map<ByteArray, Set<Utxo>>[] utxos;

    public CoinGlobalState(int... currencies) {
        this(new HashSet<>(), new HashSet<>(), currencies);
    }

    public CoinGlobalState(Set<ByteArray> minters, Set<ByteArray> users, int... currencies) {
        this.minters = Set.copyOf(minters);
        this.users = new HashSet<>(users);
        this.utxos = new Map[256];
        this.currencies = Arrays.copyOf(currencies, currencies.length);
        Arrays.sort(this.currencies);
        validate();
        initShards();
    }

    private void validate() {
        //@formatter:off
        if (Arrays.stream(currencies).anyMatch(it -> it <= 0 && it >= 255))          throw new IllegalArgumentException("Illegal currency detected " + Arrays.toString(currencies));
        if (minters.stream().anyMatch(it -> it.bytes.length != ISSUER_SIZE))         throw new IllegalArgumentException("Illegal minter detected " + minters);
        if (users.stream().anyMatch(it -> it.bytes.length != ISSUER_SIZE))           throw new IllegalArgumentException("Illegal user detected " + users);
        //@formatter:on
    }

    private void initShards() {
        // os mineradores também são usuários
        users.addAll(minters);

        for (int currency : currencies) {
            Map<ByteArray, Set<Utxo>> shard = new HashMap<>();
            users.forEach(it -> shard.put(it, new HashSet<>()));
            this.utxos[currency] = shard;
        }
    }

    public boolean isMinter(ByteArray minter) {
        //@formatter:off
        if (minter == null || minter.length == 0)       throw new IllegalArgumentException();
        //@formatter:on
        return minters.contains(minter);
    }

    public void addMinter(ByteArray minter) {
        //@formatter:off
        if (minter == null || minter.length != ISSUER_SIZE) throw new IllegalArgumentException("Illegal minter detected " + minter);
        //@formatter:on
        this.minters.add(minter);
        addUser(minter);
    }

    public void addUser(ByteArray user) {
        //@formatter:off
        if (user == null || user.length != ISSUER_SIZE) throw new IllegalArgumentException("Illegal user detected " + user);
        //@formatter:on
        this.users.add(user);
        for (int currency : currencies) {
            this.utxos[currency].put(user, new HashSet<>());
        }
    }


    public boolean isUser(ByteArray user) {
        return users.contains(user);
    }

    public boolean isCurrency(int currency) {
        return Arrays.binarySearch(currencies, currency) >= 0;
    }

    public Set<Utxo> getUtxos(int currency, ByteArray owner) {
        //@formatter:off
        if (currency < 0 || currency >= utxos.length || utxos[currency] == null)    throw new IllegalArgumentException();
        if (owner == null || owner.length != ISSUER_SIZE)                           throw new IllegalArgumentException();
        //@formatter:on
        Set<Utxo> oldUtxos = utxos[currency].get(owner);
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
        if (currency < 0 || currency >= utxos.length || utxos[currency] == null)     throw new IllegalArgumentException("Invalid currency " + currency);
        if (owner == null || owner.length != ISSUER_SIZE)                            throw new IllegalArgumentException();
        if (addresses == null)                                                       throw new IllegalArgumentException();
        //@formatter:on

        Set<Utxo> oldUtxos = utxos[currency].get(owner);
        utxos[currency].put(owner, oldUtxos.stream().filter(it -> !addresses.contains(it.address)).collect(Collectors.toSet()));
    }

    public void addUtxo(int currency, ByteArray owner, byte[] transactionHash, int outputPosition, long value) {
        Set<Utxo> oldUtxos = utxos[currency].get(owner);
        oldUtxos.add(new Utxo(transactionHash, outputPosition, value));
    }

    @Override
    public String toString() {
        return "CoinGlobalState{" +
                "currencies=" + Arrays.toString(currencies) +
                ", minters=[" + minters.stream().map(it->it.bytes).map(ByteUtils::convertToText).collect(Collectors.joining(", ")) +
                "], users=[" + users.stream().map(it->it.bytes).map(ByteUtils::convertToText).collect(Collectors.joining(", ")) +
                "], utxos=" + Arrays.toString(utxos) +
                '}';
    }
}
