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

    public static CoinGlobalState readFrom(DataInputStream dis) throws IOException {
        var mintersSize = dis.readUnsignedByte();
        var minters = new HashSet<ByteArray>(mintersSize);
        for (int i = 0; i < mintersSize; i++) {
            int length = dis.readUnsignedByte();
            var minter = dis.readNBytes(length);
            minters.add(new ByteArray(minter));
        }
        //utxos
        var shards = new Map[dis.readUnsignedByte()];
        for (int i = 0; i < shards.length; i++) {
            var shardSize = dis.readInt();
            shards[i] = new HashMap<ByteArray, Set<Utxo>>(shardSize);
            for (int j = 0; j < shardSize; j++) {
                var pkLength = dis.readUnsignedByte();
                var userPubKey = dis.readNBytes(pkLength);
                var utxosLength = dis.readInt();
                var utxos = new HashSet<Utxo>(utxosLength);
                for (int k = 0; k < utxosLength; k++) {
                    utxos.add(Utxo.readFrom(dis));
                }
                ((Map<ByteArray, Set<Utxo>>) shards[i]).put(new ByteArray(userPubKey), utxos);
            }
        }

        return new CoinGlobalState(minters, (Map<ByteArray, Set<Utxo>>[]) shards);
    }

    public void writeTo(DataOutputStream dos) throws IOException {
        dos.write(minters.size());
        for (ByteArray minter : minters) {
            dos.write(minter.length);
            dos.write(minter.bytes);
        }
        // utxos
        dos.write(shards.length);
        for (var shard : shards) {
            dos.writeInt(shard.entrySet().size());
            for (var userAccount : shard.entrySet()) {
                var userPubKey = userAccount.getKey();
                dos.write(userPubKey.length);
                dos.write(userPubKey.bytes);
                var utxos = userAccount.getValue();
                dos.writeInt(utxos.size());
                for (var utxo : utxos) {
                    utxo.writeTo(dos);
                }
            }
        }
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
