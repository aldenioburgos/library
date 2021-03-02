package demo.coin.core;

import demo.coin.core.transactions.CoinOperation;
import demo.coin.util.ByteArray;

import java.util.*;
import java.util.stream.Collectors;

import static demo.coin.core.transactions.CoinOperation.ISSUER_SIZE;

public class CoinGlobalState {

    private final int[] currencies;  // 0 .. 255 (byte sized currency)
    private final Set<ByteArray> minters;
    private final Set<ByteArray> users;
    private final Map<ByteArray, Set<Utxo>>[] utxos;

    public CoinGlobalState() {
        this(new HashSet<>(), new HashSet<>());
    }

    public CoinGlobalState(Set<byte[]> minters, Set<byte[]> users, int... currencies) {
        this.minters = minters.stream().map(ByteArray::new).collect(Collectors.toSet());
        this.users = users.stream().map(ByteArray::new).collect(Collectors.toSet());
        this.utxos = new Map[255];
        this.currencies = Arrays.copyOf(currencies, currencies.length);
        Arrays.sort(this.currencies);
        validate();
        initShards();
    }

    private void validate() {
        if (Arrays.stream(currencies).anyMatch(it -> it > 255))
            throw new IllegalArgumentException("Illegal currency detected " + Arrays.toString(currencies));
        if (minters.stream().anyMatch(it -> it.bytes.length != ISSUER_SIZE))
            throw new IllegalArgumentException("Illegal minter detected " + minters);
        if (users.stream().anyMatch(it -> it.bytes.length != ISSUER_SIZE))
            throw new IllegalArgumentException("Illegal user detected " + users);
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

    public boolean isMinter(byte[] minter) {
        return minters.contains(new ByteArray(minter));
    }

    public boolean isUser(byte[] user) {
        return users.contains(new ByteArray(user));
    }

    public boolean isCurrency(byte currency) {
        return Arrays.binarySearch(currencies, currency) >= 0;
    }

    public Set<Utxo> listUtxos(byte[] owner, int currency) {
        if (currency > utxos.length || utxos[currency] == null)
            throw new RuntimeException("Invalid currency " + currency);
        Set<Utxo> oldUtxos = utxos[currency].get(new ByteArray(owner));
        //noinspection unchecked
        return (oldUtxos == null) ? Collections.EMPTY_SET : Set.copyOf(oldUtxos);
    }

    public void removeUtxos(int currency, byte[] owner, Set<UtxoAddress> addresses) {
        if (currency > utxos.length || utxos[currency] == null)
            throw new RuntimeException("Invalid currency " + currency);

        Set<Utxo> oldUtxos = utxos[currency].get(new ByteArray(owner));
        Set<Utxo> utxosToRemove = addresses.stream().map(Utxo::new).collect(Collectors.toSet());
        oldUtxos.removeAll(utxosToRemove);
    }

    public void addUtxo(int currency, byte[] owner, byte[] transactionHash, int outputPosition, long value) {
        Set<Utxo> oldUtxos = utxos[currency].get(new ByteArray(owner));
        oldUtxos.add(new Utxo(transactionHash, outputPosition, value));
    }
}
