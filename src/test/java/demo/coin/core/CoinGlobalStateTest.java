package demo.coin.core;

import demo.coin.core.transactions.Balance;
import demo.coin.util.ByteArray;
import demo.coin.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import static demo.coin.util.CryptoUtil.generateKeyPair;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;

public class CoinGlobalStateTest {

    KeyPair   keypair;
    ByteArray pubkey;
    KeyPair   otherKeypair;
    ByteArray otherPubkey;

    @BeforeEach
    void setUp() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        keypair = generateKeyPair();
        pubkey = new ByteArray(keypair.getPublic().getEncoded());
        otherKeypair = generateKeyPair();
        otherPubkey = new ByteArray(otherKeypair.getPublic().getEncoded());
    }

    @Test
    void isMinterNoMinter() {
        var globalState = new CoinGlobalState();
        assertThrows(IllegalArgumentException.class, () -> globalState.isMinter(null));
        assertThrows(IllegalArgumentException.class, () -> globalState.isMinter(new ByteArray(new byte[]{})));
        assertFalse(globalState.isMinter(pubkey));
        assertFalse(globalState.isUser(pubkey));
        assertFalse(globalState.isMinter(otherPubkey));
        assertFalse(globalState.isUser(otherPubkey));
    }

    @Test
    void isMinterWithMinter() {
        var globalState = new CoinGlobalState(Set.of(pubkey), emptySet(),  0);
        assertFalse(globalState.isMinter(otherPubkey));
        assertTrue(globalState.isMinter(pubkey));
    }

    @Test
    void isUserWithUser() {
        var globalState = new CoinGlobalState(Set.of(pubkey), emptySet(),  0);
        assertTrue(globalState.isUser(pubkey));
        assertFalse(globalState.isUser(otherPubkey));
    }

    @Test
    void isCurrencyWithCurrency() {
        var globalState = new CoinGlobalState(Set.of(pubkey), emptySet(),  0);
        assertFalse(globalState.isCurrency(-1));
        assertFalse(globalState.isCurrency(1));
        assertTrue(globalState.isCurrency(0));
    }

    @Test
    void isCurrencyNoCurrency() {
        var emptyGlobalState = new CoinGlobalState();
        assertFalse(emptyGlobalState.isCurrency((byte) 0));
    }

    @Test
    void addListAndListUtxo() {

        var globalState = new CoinGlobalState(Set.of(pubkey), emptySet(), 2);
        assertEquals(0, globalState.getUtxos(0, pubkey).size());

        globalState.addUtxo(0, pubkey, CryptoUtil.hash(pubkey.bytes), 0, 1L);
        assertEquals(0, globalState.getUtxos(1, pubkey).size());
        assertEquals(1, globalState.getUtxos(0, pubkey).size());

        globalState.removeUtxos(1, pubkey, Set.of(new UtxoAddress(CryptoUtil.hash(pubkey.bytes), 0)));
        assertEquals(0, globalState.getUtxos(1, pubkey).size());
    }
}
