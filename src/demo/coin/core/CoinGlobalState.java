package demo.coin.core;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static java.util.Collections.*;

public class CoinGlobalState {

    private final Set<byte[]> minters =  Collections.synchronizedSet(new HashSet<>());
    private final Map<byte[], Set<Utxo>>[] utxos = new Map[Byte.MAX_VALUE];
    private final ReentrantReadWriteLock[] shardLocks = new ReentrantReadWriteLock[Byte.MAX_VALUE];

    public CoinGlobalState() {

    }

    public boolean isMinter(byte[] minter) {
        return minters.contains(minter);
    }

    public void addMinter(byte[] minter) {
        minters.add(minter);
    }

    public Set<Utxo> listUtxos(byte[] owner, byte currency) {
        Set<Utxo> oldUtxos = getUtxos(owner, currency);
        //noinspection unchecked
        return (oldUtxos == null) ? EMPTY_SET : Set.copyOf(oldUtxos);
    }

    public void removeUtxos(byte[] owner, Set<UtxoAddress> addresses, byte currency) {
        Set<Utxo> oldUtxos = getUtxos(owner, currency);

        if (oldUtxos != null) {
            Set<Utxo> utxosToRemove = addresses.stream().map(Utxo::new).collect(Collectors.toSet());
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (oldUtxos) {
                oldUtxos.removeAll(utxosToRemove);
            }
        }
    }


    public void addUtxo(byte[] owner, byte[] transactionHash, byte outputPosition, long value, byte currency) {
        var newUtxo = new Utxo(transactionHash, outputPosition, value);

        Set<Utxo> oldUtxos = getUtxos(owner, currency);
        if (oldUtxos == null) {
            Set<Utxo> newUtxos = new HashSet<>();
            newUtxos.add(newUtxo);

            getShardLock(currency).writeLock().lock();
            getShard(currency).put(owner, newUtxos);
            getShardLock(currency).writeLock().unlock();

        } else {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (oldUtxos) {
                oldUtxos.add(newUtxo);
            }
        }
    }

    private Set<Utxo> getUtxos(byte[] owner, byte currency) {
        Map<byte[], Set<Utxo>> shard = getShard(currency);

        getShardLock(currency).readLock().lock();
        Set<Utxo> oldUtxos = shard.get(owner);
        getShardLock(currency).readLock().unlock();
        return oldUtxos;
    }

    private Map<byte[], Set<Utxo>> getShard(byte currency) {
        if (utxos[currency] == null) {
            utxos[currency] = new HashMap<>();
            shardLocks[currency] = new ReentrantReadWriteLock();
        }
        return utxos[currency];
    }

    private ReentrantReadWriteLock getShardLock(byte currency) {
        return shardLocks[currency];
    }
}
