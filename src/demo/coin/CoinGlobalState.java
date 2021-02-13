package demo.coin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoinGlobalState {

    private List<byte[]> Minters = new ArrayList<>();
    private Map<byte[], List<Utxo>> utxos = new HashMap<>();


    private class Utxo {

    }
}
