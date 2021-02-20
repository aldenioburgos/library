package demo.coin.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Exchange extends Transfer {

    public Exchange() {
    }

    public Exchange(byte[] issuer, List<Input> inputs, List<Transfer.Output> outputs, byte currency) {
        super(issuer, inputs, outputs, currency);
    }

    @Override
    protected void loadDataFrom(DataInputStream dis) throws IOException {
        currency = dis.readByte();
        // inputs
        this.inputs = readInputs(dis);
        this.outputs = readOutputs(dis);
        //TODO continuar daqui.
    }

    @Override
    protected void writeDataTo(DataOutputStream dos) throws IOException {
        dos.writeByte(currency);

        // inputs
        dos.writeByte(inputs.size());
        for (Input input : inputs) {
            input.write(dos);
        }
        //outputs
        dos.writeByte(outputs.size());
        for (Transfer.Output output : outputs) {
            output.write(dos);
        }
    }

    @Override
    public boolean isInvalid(CoinGlobalState globalState) {
        return super.isInvalid(globalState);
    }


    @Override
    protected byte getOpType() {
        return EXCHANGE_TYPE;
    }

    @Override
    public void execute(CoinGlobalState globalState) {

    }

    @Override
    public String toString() {
        return "Exchange{" +
                "issuer=" + Arrays.toString(issuer) +
                ", signature=" + Arrays.toString(signature) +
                ", inputs=" + inputs +
                ", outputs=" + outputs +
                '}';
    }



    protected static class Output extends Transfer.Output {
        private byte currency;

        public Output(byte[] receiverPubKey, Long value, byte currency) {
            super(receiverPubKey, value);
            this.currency = currency;
        }

        static Exchange.Output read(DataInputStream dis) throws IOException {
            var auxReceiver = dis.readNBytes(ISSUER_SIZE);
            var auxValue = dis.readLong();
            var auxCurrency = dis.readByte();
            return new Exchange.Output(auxReceiver, auxValue, auxCurrency);
        }

        void write(DataOutputStream dos) throws IOException {
            super.write(dos);
            dos.writeByte(currency);
        }

        public Long getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Output{" +
                    "receiverPubKey=" + Arrays.toString(receiverPubKey) +
                    ", value=" + value +
                    '}';
        }
    }


}
