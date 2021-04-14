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
    void constructorTest() {
        assertDoesNotThrow(() -> new CoinGlobalState(Set.of(new ByteArray(new byte[]{0})), emptySet(), 1));
        assertThrows(IllegalArgumentException.class, () -> new CoinGlobalState(emptySet(), emptySet(), 1));
        assertThrows(IllegalArgumentException.class, () -> new CoinGlobalState(Set.of(new ByteArray(new byte[]{0})), null, 1));
        assertThrows(IllegalArgumentException.class, () -> new CoinGlobalState(Set.of(new ByteArray(new byte[]{0})), emptySet(), 0));
    }

    @Test
    void isMinterNoMinter() {
        var globalState = new CoinGlobalState(Set.of(new ByteArray(new byte[]{0})), emptySet(), 1);
        assertThrows(IllegalArgumentException.class, () -> globalState.isMinter(null));
        assertThrows(IllegalArgumentException.class, () -> globalState.isMinter(new ByteArray(new byte[]{})));
        assertFalse(globalState.isMinter(pubkey));
        assertFalse(globalState.isUser(pubkey));
        assertFalse(globalState.isMinter(otherPubkey));
        assertFalse(globalState.isUser(otherPubkey));
    }

    @Test
    void isMinterWithMinter() {
        var globalState = new CoinGlobalState(Set.of(pubkey), emptySet(),  1);
        assertFalse(globalState.isMinter(otherPubkey));
        assertTrue(globalState.isMinter(pubkey));
    }

    @Test
    void isUserWithUser() {
        var globalState = new CoinGlobalState(Set.of(pubkey), emptySet(),  1);
        assertTrue(globalState.isUser(pubkey));
        assertFalse(globalState.isUser(otherPubkey));
    }

    @Test
    void isCurrencyWithCurrency() {
        var globalState = new CoinGlobalState(Set.of(pubkey), emptySet(),  1);
        assertFalse(globalState.isCurrency(-1));
        assertFalse(globalState.isCurrency(1));
        assertTrue(globalState.isCurrency(0));
    }

}
