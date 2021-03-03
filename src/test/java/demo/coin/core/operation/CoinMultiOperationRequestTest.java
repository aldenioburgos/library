package demo.coin.core.operation;

import bftsmart.tom.core.messages.TOMMessage;
import demo.coin.core.transactions.Mint;
import demo.coin.util.CryptoUtil;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

class CoinMultiOperationRequestTest {


    @Test
    void addGetOpGetNumOps() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var request = new CoinMultiOperationRequest();
        var keyPair = CryptoUtil.generateKeyPair();
        var mint = new Mint(keyPair, 0, 1L);
        assertEquals(0, request.getNumOps());
        request.add(mint);
        assertEquals(1, request.getNumOps());
        assertArrayEquals(mint.toByteArray(), request.getOp(0));
    }


    @Test
    void serializeRebuild() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var request = new CoinMultiOperationRequest();
        var keyPair1 = CryptoUtil.generateKeyPair();
        var keyPair2 = CryptoUtil.generateKeyPair();
        var mint = new Mint(keyPair1, 0, 10L);
        var mint2 = new Mint(keyPair2, 0, 10L);
        request.add(mint);
        request.add(mint2);
        var serial = request.serialize();
        var tomMessage = new TOMMessage(0,0,0,serial,0);
        var newRequest = new CoinMultiOperationRequest(tomMessage);
        assertEquals(request, newRequest);
    }
}
