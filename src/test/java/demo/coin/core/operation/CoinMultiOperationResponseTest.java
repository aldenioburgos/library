package demo.coin.core.operation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoinMultiOperationResponseTest {

    @Test
    void serialize() {
    }

    @Test
    void isComplete() {
    }

    @Test
    void add() {
        var resp1 = new CoinMultiOperationResponse(10);
        resp1.add(0, new byte[]{0});
        resp1.add(1, new byte[]{0});
        resp1.add(2, new byte[]{0});
        resp1.add(3, new byte[]{0});
        resp1.add(4, new byte[]{0});
        resp1.add(5, new byte[]{0});
        resp1.add(6, new byte[]{0});
        resp1.add(7, new byte[]{0});
        resp1.add(8, new byte[]{0});
        resp1.add(9, new byte[]{0});
        assertThrows(IllegalArgumentException.class, () -> resp1.add(-1, new byte[]{0}));
        assertThrows(IllegalArgumentException.class, () -> resp1.add(11, new byte[]{0}));
        assertThrows(IllegalArgumentException.class, () -> resp1.add(0, null));
        assertTrue(resp1.isComplete());
        var serial = resp1.serialize();
        var resp2 = new CoinMultiOperationResponse(serial);
        assertTrue(resp2.isComplete());
        assertEquals(resp1, resp2);
    }
}
