package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Balance extends CoinOperation {


    private byte[] currencies;

    @Override
    protected OP_TYPE getOpType() {
        return OP_TYPE.BALANCE;
    }

    @Override
    public void execute(CoinGlobalState globalState) {

    }

    @Override
    protected void loadDataFrom(DataInputStream dis) throws IOException {
        byte length = dis.readByte();
        currencies = dis.readNBytes(length);
    }

    @Override
    protected void writeDataTo(DataOutputStream dos) throws IOException {
        dos.writeByte(currencies.length);
        dos.write(currencies);
    }
}
