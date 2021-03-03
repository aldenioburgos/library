package demo.coin.core.transactions;

import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

import static demo.coin.util.CryptoUtil.generateKeyPair;
import static org.junit.jupiter.api.Assertions.*;

class BalanceTest {

    @Test
    void execute() {
    }

    @Test
    void loadDataFromWriteTo() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var keypair = generateKeyPair();
        var b1 = new Balance(keypair, 0, 2, 4);
        var bytes = b1.toByteArray();
        var b2 = new Balance(bytes);
        assertEquals(b1, b2);
    }
}
