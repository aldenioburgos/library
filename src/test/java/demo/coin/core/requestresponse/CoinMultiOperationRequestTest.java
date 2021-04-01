package demo.coin.core.requestresponse;

import bftsmart.tom.core.messages.TOMMessage;
import demo.coin.util.CryptoUtil;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoinMultiOperationRequestTest {

//
//    @Test
//    void addGetOpGetNumOps() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
//        var request = new CoinMultiOperationRequest();
//        var keyPair = CryptoUtil.generateKeyPair();
//        assertEquals(0, request.getNumOps());
//        assertEquals(1, request.getNumOps());
//    }
//
//
//    @Test
//    void serializeRebuild() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
//        var request = new CoinMultiOperationRequest();
//        var keyPair1 = CryptoUtil.generateKeyPair();
//        var keyPair2 = CryptoUtil.generateKeyPair();
//        var serial = request.serialize();
//        var tomMessage = new TOMMessage(0,0,0,serial,0);
//        var newRequest = new CoinMultiOperationRequest(tomMessage);
//        assertEquals(request, newRequest);
//    }
}
