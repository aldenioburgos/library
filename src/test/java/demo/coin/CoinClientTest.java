package demo.coin;

import demo.coin.util.CryptoUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class coinClientTest {


    static KeyPair rootKeys;
    static CoinClient coinClient;

    @BeforeAll
    static void beforeAll() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        rootKeys = CryptoUtil.generateKeyPair();
        coinClient = new CoinClient();
    }


    @Test
    void createPartialStateTest() {
//        for (int i = 0; i < 5; i++) {
//            var partialState = coinClient.createPartialState(users, Collections.emptyList(), i + 1);
//            assertEquals(i + 1, partialState.length);
//            assertEquals(users.size(), partialState[i].size());
//            int finalI = i;
//            assertTrue(users.stream().allMatch(it -> partialState[finalI].get(it).size() == 0));
//        }
    }



    @Test
    void createOperation() {
//        Map<KeyPair, Map<CoinOperation, Pair<Integer, Long>>>[] partialState = new Map[2];
//        partialState[0] = new HashMap();
//        var senderReceiver = users.stream().limit(2).collect(Collectors.toList());
//        partialState[0].put(senderReceiver.get(0), new HashMap<>());
//        partialState[0].get(senderReceiver.get(0)).put(new Mint(senderReceiver.get(0), 0, 10), new Pair<>(0, 10L));
//        partialState[0].get(senderReceiver.get(0)).put(new Mint(senderReceiver.get(0), 0, 100), new Pair<>(0, 100L));
//        {
//            var op1 = coinClient.createOperation(senderReceiver, new int[]{0, 0}, 0, partialState);
//            assertTrue(op1.b instanceof Balance);
//        }
//        {
//
//            var op1 = coinClient.createOperation(senderReceiver, new int[]{0, 0}, 100, partialState);
//            assertTrue(op1.b instanceof Transfer);
//            assertTrue(op1.a instanceof Mint);
//            assertEquals(((Transfer) op1.b).getValue(), ((Mint)op1.a).getValue());
//            assertEquals(((Transfer) op1.b).getCurrency(), ((Mint)op1.a).getCurrency());
//        }
//        {
//            var op1 = coinClient.createOperation(senderReceiver, new int[]{0, 1}, 100, partialState);
//            assertTrue(op1.b instanceof Exchange);
//            assertTrue(op1.a instanceof Mint);
//            assertEquals(((Exchange) op1.b).getValue(), ((Mint)op1.a).getValue());
//            assertEquals(((Exchange) op1.b).getCurrency(), ((Mint)op1.a).getCurrency());
//        }
//    }
    }
}
