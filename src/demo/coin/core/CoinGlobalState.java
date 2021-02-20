package demo.coin.core;

import java.util.*;
import java.util.stream.Collectors;

public class CoinGlobalState {

    private final byte[] currencies;
    private final Set<byte[]> minters;
    private final Set<byte[]> users;
    private final Map<byte[], Set<Utxo>>[] utxos;

    public CoinGlobalState() {
        this(new HashSet<>(), new HashSet<>());
    }

    public CoinGlobalState(Set<byte[]> minters, Set<byte[]> users, byte... currencies) {
        this.minters = minters;
        this.users = users;
        this.utxos = new Map[Byte.MAX_VALUE];
        this.currencies = currencies;
        initShards();
    }

    private void initShards() {
        // os mineradores também são usuários
        users.addAll(minters);

        for (byte currency : currencies) {
            Map<byte[], Set<Utxo>> shard = new HashMap<>();
            users.forEach(it -> shard.put(it, new HashSet<>()));
            this.utxos[currency] = shard;
        }
    }

    public boolean isMinter(byte[] minter) {
        return minters.contains(minter);
    }

    public boolean isUser(byte[] user){
        return users.contains(user);
    }

    public boolean isCurrency(byte currency){
        for (byte item : currencies){
            if (item == currency) return true;
        }
        return false;
    }

    public Set<Utxo> listUtxos(byte[] owner, byte currency) {
        Set<Utxo> oldUtxos = utxos[currency].get(owner);
        //noinspection unchecked
        return (oldUtxos == null) ? Collections.EMPTY_SET : Set.copyOf(oldUtxos);
    }

    public void removeUtxos(byte[] owner, Set<UtxoAddress> addresses, byte currency) {
        Set<Utxo> oldUtxos =  utxos[currency].get(owner);
        Set<Utxo> utxosToRemove = addresses.stream().map(Utxo::new).collect(Collectors.toSet());
        oldUtxos.removeAll(utxosToRemove);
    }

    public void addUtxo(byte[] owner, byte[] transactionHash, byte outputPosition, long value, byte currency) {
        Set<Utxo> oldUtxos =  utxos[currency].get(owner);
        oldUtxos.add(new Utxo(transactionHash, outputPosition, value));
    }
}
