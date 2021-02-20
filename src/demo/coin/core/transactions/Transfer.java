package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.core.Utxo;
import demo.coin.core.UtxoAddress;
import demo.coin.util.ByteUtils;
import demo.coin.util.CryptoUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Transfer extends CoinOperation {

    // data
    protected byte currency;
    protected List<Input> inputs;
    protected List<? extends Output> outputs;


    protected Transfer() {
    }

    public Transfer(byte[] issuer, List<Input> inputs, List<? extends Output> outputs, byte currency) {
        super(issuer);
        this.currency = currency;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    @Override
    protected void loadDataFrom(DataInputStream dis) throws IOException {
        this.currency = dis.readByte();
        this.inputs = ByteUtils.readList(dis, Input::read);
        this.outputs = ByteUtils.readList(dis, Output::read);
    }


    @Override
    protected void writeDataTo(DataOutputStream dos) throws IOException {
        dos.writeByte(currency);
        ByteUtils.writeList(dos, inputs);
        ByteUtils.writeList(dos, outputs);
    }

    @Override
    protected OP_TYPE getOpType() {
        return OP_TYPE.TRANSFER;
    }

    @Override
    public boolean isInvalid(CoinGlobalState globalState) {
        if (!globalState.isUser(issuer)) return false;
        if (!globalState.isCurrency(currency)) return false;

        boolean validInputs = isValidInputs(globalState);
        boolean validOutputs = isValidOutputs(globalState);


        // a soma das entradas é igual a soma das saídas?
        if (validInputs && validOutputs) {
            Set<Utxo> coins = globalState.listUtxos(issuer, currency);
            var usedCoins = coins.stream().filter(it -> inputs.contains(new Input(it.getTransactionHash(), it.getOutputPosition())));
            long inputsTotalValue = usedCoins.map(Utxo::getValue).reduce(0L, Long::sum);
            long outputsTotalValue = outputs.stream().map(Output::getValue).reduce(0L, Long::sum);
            validOutputs = inputsTotalValue == outputsTotalValue;
        }

        return super.isInvalid(globalState) || !validInputs || !validOutputs;
    }

    protected boolean isValidOutputs(CoinGlobalState globalState) {
        // a transação tem saídas? Os arrays tem o tamanho certo e as saídas tem valor positivo?
        return outputs.size() > 0 && outputs.stream().allMatch(it -> it.receiverPubKey.length == ISSUER_SIZE && it.value > 0);
    }

    protected boolean isValidInputs(CoinGlobalState globalState) {
        // A transação tem entradas?
        boolean validInputs = inputs.size() > 0;

        // As entradas tem o tamanho certo e um indice de saída válido?
        validInputs = validInputs && inputs.stream().allMatch(it -> it.transactionHash.length == HASH_SIZE && it.outputIndex >= 0);

        // existe entrada repetida?
        validInputs = validInputs && inputs.stream().distinct().count() == inputs.size();

        // todas as entradas foram encontradas?
        if (validInputs) {
            Set<Utxo> coins = globalState.listUtxos(issuer, currency);
            validInputs = inputs.stream().map(it -> new Utxo(it.transactionHash, it.outputIndex)).allMatch(coins::contains);
        }
        return validInputs;
    }

    @Override
    public void execute(CoinGlobalState globalState) {
        // a transação é valida?
        if (isInvalid(globalState)) {
            throw new IllegalArgumentException("Transação inválida <" + this + ">.");
        }

        // atualiza o global state
        // consumir os utxos de entrada.
        globalState.removeUtxos(issuer, inputs.stream().map(it -> new UtxoAddress(it.transactionHash, it.outputIndex)).collect(Collectors.toSet()), currency);
        // criar os utxos de saída.
        for (int i = 0; i < outputs.size(); i++) {
            Output output = outputs.get(i);
            globalState.addUtxo(output.receiverPubKey, CryptoUtil.hash(toByteArray()), (byte) i, output.value, currency);
        }
    }


    @Override
    public String toString() {
        return "Transfer{" +
                super.toString() +
                ", inputs=" + inputs +
                ", outputs=" + outputs +
                '}';
    }

    protected static class Input implements ByteUtils.Writable {
        byte[] transactionHash;
        byte outputIndex;

        private Input(byte[] transactionHash, byte outputIndex) {
            this.transactionHash = transactionHash;
            this.outputIndex = outputIndex;
        }

        static Input read(DataInputStream dis) {
            try {
                var auxTransactionHash = dis.readNBytes(HASH_SIZE);
                var auxOutputIndex = dis.readByte();
                return new Input(auxTransactionHash, auxOutputIndex);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void writeTo(DataOutputStream dos) {
            try {
                dos.write(transactionHash);
                dos.writeByte(outputIndex);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

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

    protected static class Output implements ByteUtils.Writable {
        protected byte[] receiverPubKey;
        protected Long value;

        public Output(byte[] receiverPubKey, Long value) {
            this.receiverPubKey = receiverPubKey;
            this.value = value;
        }

        static Output read(DataInputStream dis) {
            try {
                var auxReceiver = dis.readNBytes(ISSUER_SIZE);
                var auxValue = dis.readLong();
                return new Output(auxReceiver, auxValue);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void writeTo(DataOutputStream dos) {
            try {
                dos.write(receiverPubKey);
                dos.writeLong(value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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

