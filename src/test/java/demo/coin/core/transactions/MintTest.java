package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.core.Utxo;
import demo.coin.util.ByteArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static demo.coin.util.CryptoUtil.generateKeyPair;
import static demo.coin.util.CryptoUtil.hash;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;

class MintTest {

    KeyPair keyPair;
    ByteArray pubkey;

    @BeforeEach
    void setUp() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        keyPair = generateKeyPair();
        pubkey = new ByteArray(keyPair.getPublic().getEncoded());
    }

    @Test
    void loadDataFromAndWriteDataTo() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var keypair1 = generateKeyPair();
        var mint1    = new Mint(keypair1, 0, 10L);
        var bytes1   = mint1.toByteArray();
        var newMint1 = new Mint(bytes1);
        assertEquals(mint1, newMint1);

        var keypair2 = generateKeyPair();
        var mint2    = new Mint(keypair2, 1, 99L);
        var bytes2   = mint2.toByteArray();
        Arrays.toString(bytes2);
        var newMint2 = new Mint(bytes2);
        assertEquals(mint2, newMint2);

        assertNotEquals(mint2, newMint1);
        assertNotEquals(mint1, mint2);
        assertNotEquals(mint1, newMint2);
    }


    @Test
    void isInvalid() {
        var mint = new Mint(keyPair, 1, 10L);
        { // minerador desconhecido
            var globalState = new CoinGlobalState(emptySet(), emptySet(), 1);
            assertThrows(IllegalArgumentException.class, () -> mint.validate(globalState));
        }
        { // moeda desconhecida
            var globalState = new CoinGlobalState(Set.of(pubkey), emptySet(), 4);
            assertThrows(IllegalArgumentException.class, () -> mint.validate(globalState));
        }
    }

    @Test
    void cantCreateInvalidMint() {
        assertThrows(IllegalArgumentException.class, () -> new Mint(keyPair, -1, 10L));
        assertThrows(IllegalArgumentException.class, () -> new Mint(keyPair, 256, 10L));
        assertThrows(IllegalArgumentException.class, () -> new Mint(keyPair, 0, -10L));
        assertThrows(IllegalArgumentException.class, () -> new Mint(keyPair, 0, 0L));
    }

    @Test
    void isValid() {
        var mint        = new Mint(keyPair, 255, 10L);
        var globalState = new CoinGlobalState(Set.of(pubkey), emptySet(), 255);
        assertDoesNotThrow(() -> mint.validate(globalState));
    }

    @Test
    void execute() {
        var       globalState = new CoinGlobalState(Set.of(pubkey), emptySet(), 4);
        Utxo      utxo1, utxo2;
        Set<Utxo> expected;
        { // correct work
            var mint = new Mint(keyPair, 0, 10L);
            expected = Set.of(new Utxo(hash(mint.toByteArray()), 0, 10));
            assertArrayEquals(CoinOperation.ok(), mint.execute(globalState));
            assertEquals(expected, globalState.getUtxos(0, pubkey));
        }
        { // grant indempotence
            var mint = new Mint(keyPair, 1, 10L);
            expected = Set.of(new Utxo(hash(mint.toByteArray()), 0, 10));
            assertArrayEquals(CoinOperation.ok(), mint.execute(globalState));
            assertArrayEquals(CoinOperation.ok(), mint.execute(globalState));
            assertArrayEquals(CoinOperation.ok(), mint.execute(globalState));
            assertArrayEquals(CoinOperation.ok(), mint.execute(globalState));
            assertArrayEquals(CoinOperation.ok(), mint.execute(globalState));
            assertArrayEquals(CoinOperation.ok(), mint.execute(globalState));
            assertEquals(expected, globalState.getUtxos(1, pubkey));
        }
        {
            var mint1 = new Mint(keyPair, 2, 11L);
            var mint2 = new Mint(keyPair, 2, 12L);
            utxo1 = new Utxo(hash(mint1.toByteArray()), 0, 11L);
            utxo2 = new Utxo(hash(mint2.toByteArray()), 0, 12L);
            assertArrayEquals(CoinOperation.ok(), mint2.execute(globalState));
            assertArrayEquals(CoinOperation.ok(), mint1.execute(globalState));
            expected = Set.of(utxo1, utxo2);
            assertEquals(expected, globalState.getUtxos(2, pubkey));
        }
        {
            assertEquals(emptySet(), globalState.getUtxos(3, pubkey));
        }
    }
}
