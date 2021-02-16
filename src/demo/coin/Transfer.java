package demo.coin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Transfer extends CoinOperation {

    public static final byte OP_TYPE = 1;
    private List<Input> inputs;
    private List<Output> outputs;


    public Transfer() {
    }

    public Transfer(byte[] issuer, byte[] signature, List<Input> inputs, List<Output> outputs) {
        super(issuer, signature);
        this.inputs = inputs;
        this.outputs = outputs;
    }

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
        writeDataTo(dos);
    }

    private void writeDataTo(DataOutputStream dos) throws IOException {
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
    public boolean isValid(CoinGlobalState globalState) {
        boolean validInputs = isValidInputs(globalState);
        boolean validOutputs = isValidOutputs(globalState);

        // TODO a assinatura do emissor da transação está correta?

        // a soma das entradas é igual a soma das saídas?
        if (validInputs && validOutputs) {
            Set<Utxo> coins = globalState.getUtxos(issuer);
            var usedCoins = coins.stream().filter(it -> inputs.contains(new Input(it.getTransactionHash(), it.getOutputPosition())));
            long inputsTotalValue = usedCoins.map(Utxo::getValue).reduce(0L, Long::sum);
            long outputsTotalValue = outputs.stream().map(Output::getValue).reduce(0L, Long::sum);
            validOutputs = inputsTotalValue == outputsTotalValue;
        }

        return super.isValid(globalState) && validInputs && validOutputs;
    }

    private boolean isValidOutputs(CoinGlobalState globalState) {
        // a transação tem saídas? Os arrays tem o tamanho certo e as saídas tem valor positivo?
        return outputs.size() > 0 && outputs.stream().allMatch(it -> it.receiverPubKey.length == 32 && it.value > 0);
    }

    private boolean isValidInputs(CoinGlobalState globalState) {
        // A transação tem entradas?
        boolean validInputs = inputs.size() > 0;

        // As entradas tem o tamanho certo e um indice de saída válido?
        validInputs = validInputs && inputs.stream().allMatch(it -> it.transactionHash.length == 32 && it.outputIndex >= 0);

        // existe entrada repetida?
        validInputs = validInputs && inputs.stream().distinct().count() == inputs.size();

        // todas as entradas foram encontradas?
        if (validInputs) {
            Set<Utxo> coins = globalState.getUtxos(issuer);
            validInputs = inputs.stream().map(it -> new Utxo(it.transactionHash, it.outputIndex)).allMatch(coins::contains);
        }
        return validInputs;
    }

    @Override
    public void execute(CoinGlobalState globalState) {
        // a transação é valida?
        if (!isValid(globalState)) {
            throw new IllegalArgumentException("Transação inválida <" + this + ">.");
        }

        // atualiza o global state
        // consumir os utxos de entrada.
        globalState.removeUtxos(issuer, inputs.stream().map(it -> new UtxoAddress(it.transactionHash, it.outputIndex)).collect(Collectors.toSet()));
        // criar os utxos de saída.
        for (int i = 0; i < outputs.size(); i++) {
            Output output = outputs.get(i);
            globalState.addUtxo(output.receiverPubKey, getTransactionHash(), (byte) i, output.value);
        }
    }


    @Override
    public byte[] getDataBytes() {
        return new byte[0]; //TODO
    }

    @Override
    public String toString() {
        return "Transfer{" +
                super.toString() +
                ", inputs=" + inputs +
                ", outputs=" + outputs +
                '}';
    }

    private static final class Input {
        byte[] transactionHash;
        byte outputIndex;

        private Input(byte[] transactionHash, byte outputIndex) {
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Input input = (Input) o;
            return outputIndex == input.outputIndex && Arrays.equals(transactionHash, input.transactionHash);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(outputIndex);
            result = 31 * result + Arrays.hashCode(transactionHash);
            return result;
        }

        @Override
        public String toString() {
            return "Input{" +
                    "transactionHash=" + Arrays.toString(transactionHash) +
                    ", outputIndex=" + outputIndex +
                    '}';
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

        public byte[] getReceiverPubKey() {
            return receiverPubKey;
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

