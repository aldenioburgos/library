package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.core.Utxo;
import demo.coin.core.transactions.Transfer.ContaValor;
import demo.coin.util.ByteArray;
import demo.coin.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static demo.coin.util.CryptoUtil.generateKeyPair;
import static demo.coin.util.CryptoUtil.hash;
import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

class TransferTest {

    KeyPair   minterKeys;
    KeyPair   receiverKeys;
    ByteArray receiverPubKey;
    ByteArray minterPubKey;
    Mint      mint;
    Transfer  transfer;

    @BeforeEach
    void setUp() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        minterKeys = generateKeyPair();
        receiverKeys = generateKeyPair();
        receiverPubKey = new ByteArray(receiverKeys.getPublic().getEncoded());
        minterPubKey = new ByteArray(minterKeys.getPublic().getEncoded());
        mint = new Mint(minterKeys, 0, 10L);
        transfer = new Transfer(minterKeys, 0, Map.of(mint, 0), List.of(new ContaValor(receiverPubKey, 10L)));
    }

    @Test
    void loadDataFromAndWriteDataTo() {
        var t2 = new Transfer(transfer.toByteArray());
        assertEquals(transfer, t2);
    }


    @Test
    void isInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new Transfer(null, 0, Map.of(mint, 0), List.of(new ContaValor(receiverPubKey, 10L))));
        assertThrows(IllegalArgumentException.class, () -> new Transfer(minterKeys, -1, Map.of(mint, 0), List.of(new ContaValor(receiverPubKey, 10L))));
        assertThrows(IllegalArgumentException.class, () -> new Transfer(minterKeys, 256, Map.of(mint, 0), List.of(new ContaValor(receiverPubKey, 10L))));
        assertThrows(IllegalArgumentException.class, () -> new Transfer(minterKeys, 1, null, List.of(new ContaValor(receiverPubKey, 10L))));
        assertThrows(IllegalArgumentException.class, () -> new Transfer(minterKeys, 1, emptyMap(), List.of(new ContaValor(receiverPubKey, 10L))));
        assertThrows(IllegalArgumentException.class, () -> new Transfer(minterKeys, 2, Map.of(mint, 0), null));
        assertThrows(IllegalArgumentException.class, () -> new Transfer(minterKeys, 3, Map.of(mint, 0), emptyList()));
    }

    @Test
    void execute() {
        CoinGlobalState globalState = new CoinGlobalState(Set.of(minterPubKey), Set.of(receiverPubKey), 3);
        // mint
        assertArrayEquals(CoinOperation.ok(), mint.execute(globalState));
        assertEquals(Set.of(new Utxo(hash(mint.toByteArray()), 0, 10L)), globalState.getUtxos(0, minterPubKey));
        assertEquals(emptySet(), globalState.getUtxos(1, minterPubKey));
        assertEquals(emptySet(), globalState.getUtxos(2, minterPubKey));
        assertEquals(emptySet(), globalState.getUtxos(0, receiverPubKey));
        assertEquals(emptySet(), globalState.getUtxos(1, receiverPubKey));
        assertEquals(emptySet(), globalState.getUtxos(2, receiverPubKey));

        // transfer
        assertArrayEquals(CoinOperation.ok(), transfer.execute(globalState));
        assertEquals(emptySet(), globalState.getUtxos(0, minterPubKey));
        assertEquals(emptySet(), globalState.getUtxos(1, minterPubKey));
        assertEquals(emptySet(), globalState.getUtxos(2, minterPubKey));
        assertEquals(Set.of(new Utxo(hash(transfer.toByteArray()), 0, 10L)), globalState.getUtxos(0, receiverPubKey));
        assertEquals(emptySet(), globalState.getUtxos(1, receiverPubKey));
        assertEquals(emptySet(), globalState.getUtxos(2, receiverPubKey));
    }
}
