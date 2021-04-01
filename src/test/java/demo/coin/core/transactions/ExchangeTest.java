package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.core.Utxo;
import demo.coin.core.transactions.Exchange.ContaValorMoeda;
import demo.coin.util.ByteArray;
import demo.coin.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExchangeTest {

    KeyPair   minterKeys;
    ByteArray minterAccount;
    KeyPair   receiverKeys;
    ByteArray receiverAccount;

    @BeforeEach
    void setUp() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        minterKeys = CryptoUtil.generateKeyPair();
        minterAccount = new ByteArray(minterKeys.getPublic().getEncoded());
        receiverKeys = CryptoUtil.generateKeyPair();
        receiverAccount = new ByteArray(receiverKeys.getPublic().getEncoded());
    }

//    @Test
//    void validation() {
//        var mint = new Mint(minterKeys, 0, 10);
//        assertThrows(IllegalArgumentException.class, () -> new Exchange(null));
//        assertThrows(IllegalArgumentException.class, () -> new Exchange(new byte[]{}));
//        assertThrows(IllegalArgumentException.class, () -> new Exchange(null, 0, Map.of(mint, 0), List.of(new ContaValorMoeda(receiverAccount, 10, 0))));
//        assertThrows(IllegalArgumentException.class, () -> new Exchange(minterKeys, -1, Map.of(mint, 0), List.of(new ContaValorMoeda(receiverAccount, 10, 0))));
//        assertThrows(IllegalArgumentException.class, () -> new Exchange(minterKeys, 256, Map.of(mint, 0), List.of(new ContaValorMoeda(receiverAccount, 10, 0))));
//        assertThrows(IllegalArgumentException.class, () -> new Exchange(minterKeys, 0, (Map)null, List.of(new ContaValorMoeda(receiverAccount, 10, 0))));
//        assertThrows(IllegalArgumentException.class, () -> new Exchange(minterKeys, 0, Collections.emptyMap(), List.of(new ContaValorMoeda(receiverAccount, 10, 0))));
//        assertThrows(IllegalArgumentException.class, () -> new Exchange(minterKeys, 256, Map.of(mint, 0), null));
//        assertThrows(IllegalArgumentException.class, () -> new Exchange(minterKeys, 256, Map.of(mint, 0), Collections.emptyList()));
//
//        assertThrows(IllegalArgumentException.class, () -> new Exchange(minterKeys, 0, Map.of(mint, 0), List.of(new ContaValorMoeda(receiverAccount, 10, -1))));
//        assertThrows(IllegalArgumentException.class, () -> new Exchange(minterKeys, 0, Map.of(mint, 0), List.of(new ContaValorMoeda(receiverAccount, 0, -1))));
//        assertThrows(IllegalArgumentException.class, () -> new Exchange(minterKeys, 0, Map.of(mint, 0), List.of(new ContaValorMoeda(receiverAccount, -1, -1))));
//        assertThrows(IllegalArgumentException.class, () -> new Exchange(minterKeys, 0, Map.of(mint, 0), List.of(new ContaValorMoeda(receiverAccount, 10, 256))));
//        assertThrows(IllegalArgumentException.class, () -> new Exchange(minterKeys, 0, Map.of(mint, -1), List.of(new ContaValorMoeda(receiverAccount, 10, 0))));
//        assertThrows(IllegalArgumentException.class, () -> new Exchange(minterKeys, 0, Map.of(mint, 256), List.of(new ContaValorMoeda(receiverAccount, 10, 0))));
//        assertDoesNotThrow(() -> new Exchange(minterKeys, 0, Map.of(mint, 0), List.of(new ContaValorMoeda(receiverAccount, 10, 0))));
//    }
//
//
//    @Test
//    void serialization() {
//        var mint        = new Mint(minterKeys, 0, 1000L);
//        var exchange    = new Exchange(minterKeys, 0, Map.of(mint, 0), List.of(new ContaValorMoeda(receiverAccount, 500L, 1), new ContaValorMoeda(receiverAccount, 500L, 0)));
//        var newExchange = new Exchange(exchange.toByteArray());
//        assertEquals(exchange, newExchange);
//    }
//
//    @Test
//    void execute() {
//        var globalState = new CoinGlobalState(Set.of(minterAccount), Set.of(receiverAccount), 3);
//        var mint        = new Mint(minterKeys, 0, 1000L);
//        var exchange    = new Exchange(minterKeys, 0, Map.of(mint, 0), List.of(new ContaValorMoeda(receiverAccount, 500L, 1), new ContaValorMoeda(receiverAccount, 500L, 0)));
//        assertArrayEquals(CoinOperation.ok(), mint.execute(globalState));
//        assertArrayEquals(CoinOperation.ok(), exchange.execute(globalState));
//        var expected0 = Set.of(new Utxo(exchange.hash(), 1, 500L));
//        var expected1 = Set.of(new Utxo(exchange.hash(), 0, 500L));
//        assertEquals(expected0, globalState.getUtxos(0, receiverAccount));
//        assertEquals(expected1, globalState.getUtxos(1, receiverAccount));
//    }
}
