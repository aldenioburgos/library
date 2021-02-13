package demo.coin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Transfer extends CoinOperation {

    public static final byte OP_TYPE = 1;
    private List<Input> inputs;
    private List<Output> outputs;


    @Override
    protected void loadFromWorker(DataInputStream dis) throws IOException {
        super.loadFromWorker(dis);
        // inputs
        byte numInputs = dis.readByte();
        inputs = new ArrayList<>(numInputs);
        for (int i = 0; i < numInputs; i++) {
            inputs.add(Input.read(dis));
        }
        //outputs
        byte numOutputs = dis.readByte();
        outputs = new ArrayList<>(numOutputs);
        for (int i = 0; i < numOutputs; i++) {
            outputs.add(Output.read(dis));
        }
    }

    @Override
    public void writeTo(DataOutputStream dos) throws IOException {
        super.writeTo(dos);
        // inputs
        dos.writeByte(inputs.size());
        for (Input input : inputs) {
            input.write(dos);
        }
        //outputs
        dos.writeByte(outputs.size());
        for (Output output : outputs) {
            output.write(dos);
        }
    }

    @Override
    protected byte getOpType() {
        return OP_TYPE;
    }

    @Override
    public void execute(CoinGlobalState globalState) {

    }


    private static final class Input {
        byte[] transactionHash;
        Byte outputIndex;

        private Input(byte[] transactionHash, Byte outputIndex) {
            this.transactionHash = transactionHash;
            this.outputIndex = outputIndex;
        }

        static Input read(DataInputStream dis) throws IOException {
            var auxTransactionHash = dis.readNBytes(32);
            var auxOutputIndex = dis.readByte();
            return new Input(auxTransactionHash, auxOutputIndex);
        }

        void write(DataOutputStream dos) throws IOException {
            dos.write(transactionHash);
            dos.writeByte(outputIndex);
        }
    }

    private static final class Output {
        byte[] receiverPubKey;
        Long value;

        private Output(byte[] receiverPubKey, Long value) {
            this.receiverPubKey = receiverPubKey;
            this.value = value;
        }

        static Output read(DataInputStream dis) throws IOException {
            var auxReceiver = dis.readNBytes(32);
            var auxValue = dis.readLong();
            return new Output(auxReceiver, auxValue);
        }

        void write(DataOutputStream dos) throws IOException {
            dos.write(receiverPubKey);
            dos.writeLong(value);
        }
    }
}

