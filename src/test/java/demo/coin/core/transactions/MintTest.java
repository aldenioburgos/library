package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.core.Utxo;
import demo.coin.util.CryptoUtil;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static demo.coin.util.CryptoUtil.generateKeyPair;
import static demo.coin.util.CryptoUtil.hash;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;

class MintTest {

    @Test
    void loadDataFromAndWriteDataTo() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var keypair1 = generateKeyPair();
        var mint1 = new Mint(keypair1, 0, 10L);
        var bytes1 = mint1.toByteArray();
        var newMint1 = new Mint(bytes1);
        assertEquals(mint1, newMint1);

        var keypair2 = generateKeyPair();
        var mint2 = new Mint(keypair2, 1, 99L);
        var bytes2 = mint2.toByteArray();
        Arrays.toString(bytes2);
        var newMint2 = new Mint(bytes2);
        assertEquals(mint2, newMint2);

        assertNotEquals(mint2, newMint1);
        assertNotEquals(mint1, mint2);
        assertNotEquals(mint1, newMint2);
    }


    @Test
    void isInvalid() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        { // valido
            var keypair = generateKeyPair();
            var mint = new Mint(keypair, 1, 10L);
            var globalState = new CoinGlobalState(Set.of(keypair.getPublic().getEncoded()), emptySet(), 1);
            assertFalse(mint.isInvalid(globalState));
        }
        { // minerador desconhecido
            var keypair = generateKeyPair();
            var mint = new Mint(keypair, 1, 10L);
            var globalState = new CoinGlobalState(emptySet(), emptySet(), 1);
            assertTrue(mint.isInvalid(globalState));
        }
        { // moeda invalida
            var keypair = generateKeyPair();
            var mint = new Mint(keypair, -1, 10L);
            var globalState = new CoinGlobalState(Set.of(keypair.getPublic().getEncoded()), emptySet(), 0);
            assertTrue(mint.isInvalid(globalState));
        }
        { // moeda desconhecida
            var keypair = generateKeyPair();
            var mint = new Mint(keypair, 32, 10L);
            var globalState = new CoinGlobalState(Set.of(keypair.getPublic().getEncoded()), emptySet(), 0, 1, 2, 3, 4);
            assertTrue(mint.isInvalid(globalState));
        }
        { // 0 reais
            var keypair = generateKeyPair();
            var mint = new Mint(keypair, 0, 0L);
            var globalState = new CoinGlobalState(Set.of(keypair.getPublic().getEncoded()), emptySet(), 0);
            assertTrue(mint.isInvalid(globalState));
        }
        { // reais negativos
            var keypair = generateKeyPair();
            var mint = new Mint(keypair, 0, -10L);
            var globalState = new CoinGlobalState(Set.of(keypair.getPublic().getEncoded()), emptySet(), 0);
            assertTrue(mint.isInvalid(globalState));
        }
    }

    @Test
    void execute() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var keyPair = generateKeyPair();
        var pubKey = keyPair.getPublic().getEncoded();
        var globalState = new CoinGlobalState(Set.of(pubKey), emptySet(), 0, 1, 2);
        Utxo utxo1, utxo2;
        {
            var mint = new Mint(keyPair, 0, 10L);
            utxo1 = new Utxo(hash(mint.toByteArray()), 0, 10);
            mint.execute(globalState);
            var expected = Set.of(utxo1);
            assertEquals(expected, globalState.listUtxos(pubKey, 0));
        }
        {
            var mint = new Mint(keyPair, 1, 10L);
            mint.execute(globalState);
            mint.execute(globalState);
            mint.execute(globalState);
            mint.execute(globalState);
            mint.execute(globalState);
            mint.execute(globalState);
            mint.execute(globalState);
            var expected = Set.of(new Utxo(hash(mint.toByteArray()), 0, 10));
            assertEquals(expected, globalState.listUtxos(pubKey, 1));
        }
        {
            var mint = new Mint(keyPair, 0, 11L);
            utxo2 = new Utxo(hash(mint.toByteArray()), 0, 11);
            mint.execute(globalState);
            var expected = Set.of(utxo1, utxo2);
            assertEquals(expected, globalState.listUtxos(pubKey, 0));
        }
        {
            assertEquals(emptySet(), globalState.listUtxos(pubKey, 2));
        }
    }
}
