package demo.coin.util;

import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {

    @Test
    void generateKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var keypair1 = CryptoUtil.generateKeyPair();
        assertNotNull(keypair1);
        assertNotNull(keypair1.getPrivate());
        assertNotNull(keypair1.getPublic());
        var keypair2 = CryptoUtil.generateKeyPair();
        assertNotNull(keypair2);
        assertNotNull(keypair2.getPrivate());
        assertNotNull(keypair2.getPublic());
        assertNotEquals(keypair1, keypair2);
        assertFalse(Arrays.equals(keypair1.getPublic().getEncoded(), keypair2.getPublic().getEncoded()));
        assertFalse(Arrays.equals(keypair1.getPrivate().getEncoded(), keypair2.getPrivate().getEncoded()));
    }

    @Test
    void loadPrivateKey() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeySpecException {
        var keypair1 = CryptoUtil.generateKeyPair();
        var privateKeyBytes = keypair1.getPrivate().getEncoded();
        var privateKey = CryptoUtil.loadPrivateKey(privateKeyBytes);
        assertNotNull(privateKey);
        assertEquals(keypair1.getPrivate(), privateKey);
    }

    @Test
    void loadPublicKey() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeySpecException {
        var keypair1 = CryptoUtil.generateKeyPair();
        var pubkeyBytes = keypair1.getPublic().getEncoded();
        var publicKey = CryptoUtil.loadPublicKey(pubkeyBytes);
        assertNotNull(publicKey);
        assertEquals(keypair1.getPublic(), publicKey);
    }

    @Test
    void signAndCheckSignature() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var keypair = CryptoUtil.generateKeyPair();
        var data = ("arruma a mala aearruma a mala aearruma a mala aearruma a mala aearruma a mala aearruma a mala aearruma a mala aearruma a mala ae").getBytes();
        var priKeyBytes = keypair.getPrivate().getEncoded();
        var pubKeyBytes = keypair.getPublic().getEncoded();
        var hash = CryptoUtil.hash(data);
        var signature = CryptoUtil.sign(priKeyBytes, hash);
        assertTrue(CryptoUtil.checkSignature(pubKeyBytes, hash, signature));

        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.hash(null));
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.sign(priKeyBytes, null));
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.sign(null, hash));
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.checkSignature(pubKeyBytes, hash, null));
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.checkSignature(pubKeyBytes, null, signature));
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.checkSignature(null, hash, signature));
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.checkSignature(pubKeyBytes, Arrays.copyOf(hash, hash.length+1), signature));
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.checkSignature(pubKeyBytes, Arrays.copyOf(hash, hash.length-1), signature));
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.checkSignature(Arrays.copyOf(pubKeyBytes, pubKeyBytes.length-1), hash, signature));
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.checkSignature(Arrays.copyOf(pubKeyBytes, pubKeyBytes.length+1), hash, signature));
        assertThrows(RuntimeException.class, () -> CryptoUtil.checkSignature(pubKeyBytes, hash, Arrays.copyOf(signature, signature.length-1)));
        assertThrows(RuntimeException.class, () -> CryptoUtil.checkSignature(pubKeyBytes, hash, Arrays.copyOf(signature, signature.length+1)));
    }

    @Test
    void hash() {
        var hash1 = CryptoUtil.hash("texto qualquer para fazer o hash".getBytes());
        var hash2 = CryptoUtil.hash("t".getBytes());
        assertEquals(32, hash1.length);
        assertEquals(32, hash2.length);
        assertFalse(Arrays.equals(hash1, hash2));
    }
}
