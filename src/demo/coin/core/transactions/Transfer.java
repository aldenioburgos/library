package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.core.Utxo;
import demo.coin.core.UtxoAddress;
import demo.coin.util.ByteUtils;
import demo.coin.util.ByteUtils.Writable;
import demo.coin.util.CryptoUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static demo.coin.util.ByteUtils.readByteSizedList;

//  /-----------------header---------------------\-/------------------------body------------------------\
//   <op_type>  [    <accounts>   ]   <signature>   <currency>  [<inputs>]            [<outputs>]
//   <1>         <1>(<91>..<23205>)    <71>            <1>      <1>(<33>..<8415>)     <1>(<9>..<2295>)
// total: 1      +1+(91..23205)        +71             +1       +1+(33..8415)         +1+(9..2295)       = 209..33991 bytes
public class Transfer extends CoinOperation {

    // data
    protected int currency;
    protected List<Input> inputs;
    protected List<? extends Output> outputs;

    protected Transfer() {
    }

    public Transfer(byte[] bytes) {
        load(bytes);
    }

    public Transfer(byte[] issuer, List<Input> inputs, List<? extends Output> outputs, byte currency) {
        super(issuer);
        this.currency = currency;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    @Override
    protected void validateType(DataInputStream dis) throws IOException {
        int type = dis.readUnsignedByte();
        if (type != OP_TYPE.TRANSFER.ordinal()) throw new RuntimeException("Invalid Type! " + type);
    }

    @Override
    protected void loadDataFrom(DataInputStream dis) throws IOException {
        this.currency = dis.readUnsignedByte();
        this.inputs = readByteSizedList(dis, Input::read);
        this.outputs = readByteSizedList(dis, Output::read);
    }


    @Override
    protected void writeDataTo(DataOutputStream dos) throws IOException {
        dos.writeByte(currency);
        ByteUtils.writeByteSizedList(dos, inputs);
        ByteUtils.writeByteSizedList(dos, outputs);
    }

    @Override
    protected OP_TYPE getOpType() {
        return OP_TYPE.TRANSFER;
    }

    @Override
    public boolean isInvalid(CoinGlobalState globalState) {
        if (!globalState.isUser(accounts.get(issuer))) return false;
        if (!globalState.isCurrency((byte) currency)) return false;

        boolean validInputs = isValidInputs(globalState);
        boolean validOutputs = isValidOutputs(globalState);


        // a soma das entradas é igual a soma das saídas?
        if (validInputs && validOutputs) {
            Set<Utxo> coins = globalState.listUtxos(accounts.get(issuer), currency);
            long inputsTotalValue = coins.stream()
                    .filter(it -> inputs.contains(new Input(it.getTransactionHash(), it.getOutputPosition())))
                    .map(Utxo::getValue)
                    .reduce(0L, Long::sum);
            long outputsTotalValue = outputs.stream()
                    .map(Output::getValue)
                    .reduce(0L, Long::sum);
            validOutputs = inputsTotalValue == outputsTotalValue;
        }

        return super.isInvalid(globalState) || !validInputs || !validOutputs;
    }

    protected boolean isValidOutputs(CoinGlobalState globalState) {
        // a transação tem saídas?
        if (outputs.size() <= 0) return false;

        // ao menos um dos recebedores deve ser diferente do issuer
        if (outputs.stream().allMatch(it -> it.receiverAccountIndex == issuer)) return false;

        // os arrays tem o tamanho certo e as saídas tem valor positivo?
        if (outputs.stream().anyMatch(it -> it.receiverAccountIndex >= accounts.size() || it.value <= 0)) return false;

        return true;
    }

    protected boolean isValidInputs(CoinGlobalState globalState) {
        // A transação tem entradas?
        if (inputs.size() <= 0) return false;

        // As entradas tem o tamanho certo e um indice de saída válido?
        if (inputs.stream().anyMatch(it -> it.transactionHash.length != HASH_SIZE || it.outputIndex < 0)) return false;

        // existe entrada repetida?
        if (inputs.stream().distinct().count() != inputs.size()) return false;

        // todas as entradas foram encontradas?
        Set<Utxo> coins = globalState.listUtxos(accounts.get(issuer), currency);
        return inputs.stream()
                .map(it -> new Utxo(it.transactionHash, it.outputIndex))
                .allMatch(coins::contains);
    }

    @Override
    public byte[] execute(CoinGlobalState globalState) {
        try {
            // a transação é valida?
            if (isInvalid(globalState)) throw new IllegalArgumentException("Transação inválida <" + this + ">.");

            // consumir os utxos de entrada.
            var utxoToRemove = inputs.stream()
                    .map(it -> new UtxoAddress(it.transactionHash, it.outputIndex))
                    .collect(Collectors.toSet());
            globalState.removeUtxos(accounts.get(issuer), utxoToRemove, (byte) currency);

            // criar os utxos de saída.
            byte[] transactionHash = CryptoUtil.hash(toByteArray());
            for (int i = 0; i < outputs.size(); i++) {
                Output output = outputs.get(i);
                globalState.addUtxo(this.currency, accounts.get(output.receiverAccountIndex), transactionHash, i, output.value);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return fail();
        }
        return ok();
    }


    @Override
    public String toString() {
        return "Transfer{" +
                super.toString() +
                ", inputs=" + inputs +
                ", outputs=" + outputs +
                '}';
    }

    protected static class Input implements Writable {
        protected final byte[] transactionHash;
        protected final int outputIndex;

        private Input(byte[] transactionHash, int outputIndex) {
            this.transactionHash = transactionHash;
            this.outputIndex = outputIndex;
        }

        static Input read(DataInputStream dis) {
            try {
                byte[] auxTransactionHash = dis.readNBytes(HASH_SIZE);
                int auxOutputIndex = dis.readUnsignedByte();
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
            return "{" +
                    "transactionHash=" + Arrays.toString(transactionHash) +
                    ", outputIndex=" + outputIndex +
                    '}';
        }
    }

    protected static class Output implements Writable {
        protected final int receiverAccountIndex;
        protected final Long value;

        public Output(int receiverAccountIndex, Long value) {
            this.receiverAccountIndex = receiverAccountIndex;
            this.value = value;
        }

        static Output read(DataInputStream dis) {
            try {
                int auxReceiverIndex = dis.readUnsignedByte();
                long auxValue = dis.readLong();
                return new Output(auxReceiverIndex, auxValue);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void writeTo(DataOutputStream dos) {
            try {
                dos.writeByte(receiverAccountIndex);
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
            return "{" +
                    "receiverAccountIndex=" + receiverAccountIndex +
                    ", value=" + value +
                    '}';
        }
    }
}

