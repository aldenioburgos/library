package demo.coin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Mint extends CoinOperation {

    public static final byte OP_TYPE = 0;
    private long value;


    @Override
    protected void loadFromWorker(DataInputStream dis) throws IOException {
        super.loadFromWorker(dis);
        value = dis.readLong();
    }

    @Override
    protected void writeTo(DataOutputStream dos) throws IOException {
        super.writeTo(dos);
        dos.writeLong(value);
    }

    @Override
    protected byte getOpType() {
        return OP_TYPE;
    }

    @Override
    public void execute(CoinGlobalState globalState) {

    }


}
