package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.core.Utxo;
import demo.coin.core.UtxoAddress;
import demo.coin.util.ByteArray;
import demo.coin.util.ByteUtils;
import demo.coin.util.ByteUtils.Writable;
import demo.coin.util.CryptoUtil;
import demo.coin.util.Pair;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.util.*;
import java.util.stream.Collectors;

import static demo.coin.util.ByteUtils.readByteSizedList;

//  /-----------------header---------------------\-/------------------------body------------------------\
//   <op_type>  [    <accounts>   ]   <signature>   <currency>  [<inputs>]            [<outputs>]
//   <1>         <1>(<91>..<23205>)    <71>            <1>      <1>(<33>..<8415>)     <1>(<9>..<2295>)
// total: 1      +1+(91..23205)        +71             +1       +1+(33..8415)         +1+(9..2295)       = 209..33991 bytes
public class Transfer extends CoinOperation {

    // data
    protected int                    currency;
    protected List<Input>            inputs;
    protected List<? extends Output> outputs;

    public Transfer(byte[] bytes) {
        //@formatter:off
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException();
        //@formatter:on
        load(bytes);
    }

    public Transfer(KeyPair keyPair, int currency, Map<CoinOperation, Integer> inputs, List<? extends ContaValor> outputs) {
        super(keyPair);
        //@formatter:off
        if (currency < 0 || currency > 255)                                                                             throw new IllegalArgumentException();
        if (inputs == null || inputs.isEmpty())                                                                         throw new IllegalArgumentException();
        if (inputs.keySet().stream().anyMatch(it -> it == null || it instanceof Balance))                               throw new IllegalArgumentException();
        if (inputs.values().stream().anyMatch(it -> it == null || it < 0 || it > 255))                                  throw  new IllegalArgumentException();
        if (outputs == null || outputs.isEmpty())                                                                       throw new IllegalArgumentException();
        if (outputs.stream().anyMatch(it-> it.a == null || it.a.length != ISSUER_SIZE || it.b == null || it.b < 0))     throw new IllegalArgumentException();
        //@formatter:on
        this.currency = currency;
        this.inputs = convertToInputs(inputs);
        this.outputs = convertToOutputs(outputs);
        sign(keyPair.getPrivate().getEncoded());
    }

    @Override
    public byte[] execute(CoinGlobalState globalState) {
        try {
            validate(globalState);

            // consumir os utxos de entrada.
            globalState.removeUtxos(currency, accounts.get(issuer), getInputsAsUtxoAddresses());

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
    public int getClassId(Set<SortedSet<Integer>> allPossiblePartitionsArrangement) {
        return ("["+currency+"]").hashCode();
    }

    @Override
    public void validate(CoinGlobalState globalState) {
        super.validate(globalState);
        //@formatter:off
        if (currency < 0 || currency > 255 || !globalState.isCurrency(currency))    throw new IllegalArgumentException();
        //@formatter:on

        validateInputs(globalState);
        validateOutputs(globalState);

        // busca os inputs no global state
        Set<Utxo> coins = globalState.getUtxos(currency, accounts.get(issuer), getInputsAsUtxoAddresses());

        // todas as entradas foram encontradas?
        //@formatter:off
        if (coins.size() != inputs.size())                                          throw new IllegalArgumentException();
        //@formatter:on

        // a soma das entradas é igual a soma das saídas?
        long inputsTotalValue = coins.stream()
                .filter(it -> inputs.contains(new Input(it.getTransactionHash(), it.getOutputPosition())))
                .map(Utxo::getValue)
                .reduce(0L, Long::sum);
        long outputsTotalValue = outputs.stream()
                .map(Output::getValue)
                .reduce(0L, Long::sum);
        //@formatter:off
        if (inputsTotalValue != outputsTotalValue)                                  throw new IllegalArgumentException();
        //@formatter:on

    }

    protected void validateInputs(CoinGlobalState globalState) {
        //@formatter:off
        if (inputs == null || inputs.size() <= 0)                                                                                   throw new IllegalArgumentException();
        // algum hash tem tamanho errado ou indice maior que um ubyte?
        if (inputs.stream().anyMatch(it -> it.transactionHash.length != HASH_SIZE || it.outputIndex < 0 || it.outputIndex > 255))   throw new IllegalArgumentException();
        // existe entrada repetida?
        if (inputs.stream().distinct().count() != inputs.size())                                                                    throw new IllegalArgumentException();
        //@formatter:on
    }

    protected void validateOutputs(CoinGlobalState globalState) {
        //@formatter:off
        if (outputs == null || outputs.size() <= 0)                                                         throw new IllegalArgumentException();
        // ao menos um dos recebedores deve ser diferente do issuer
        if (outputs.stream().allMatch(it -> it.receiverAccountIndex == issuer))                             throw new IllegalArgumentException();
        // os arrays tem o tamanho certo e as saídas tem valor positivo?
        if (outputs.stream().anyMatch(it -> it.receiverAccountIndex >= accounts.size() || it.value <= 0))   throw new IllegalArgumentException();
        //@formatter:on
    }

    public long getValue(){
       return outputs.stream().map(Output::getValue).reduce(0l, Long::sum);
    }

    protected List<? extends Output> convertToOutputs(List<? extends ContaValor> outputsToConvert) {
        List<Output> convertedOutputs = new ArrayList<>(outputsToConvert.size());
        for (var output : outputsToConvert) {
            convertedOutputs.add(new Output(addAccount(output.a), output.b));
        }
        return convertedOutputs;
    }


    private List<Input> convertToInputs(Map<CoinOperation, Integer> inputsToConvert) {
        List<Input> convertedInputs = new ArrayList<>(inputsToConvert.size());
        for (var input : inputsToConvert.entrySet()) {
            var transactionHash = CryptoUtil.hash(input.getKey().toByteArray());
            convertedInputs.add(new Input(transactionHash, input.getValue()));
        }
        return convertedInputs;
    }

    @Override
    protected void validateType(DataInputStream dis) throws IOException {
        int type = dis.readUnsignedByte();
        if (type != OP_TYPE.TRANSFER.ordinal())
            throw new RuntimeException("Invalid Type! " + type);
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

    protected Set<UtxoAddress> getInputsAsUtxoAddresses() {
        return inputs.stream().map(it -> new UtxoAddress(it.transactionHash, it.outputIndex)).collect(Collectors.toSet());
    }


    @Override
    public String toString() {
        return "Transfer{" +
                super.toString() +
                ", inputs=" + inputs +
                ", outputs=" + outputs +
                '}';
    }

    public static class ContaValor extends Pair<ByteArray, Long> {
        public ContaValor(KeyPair a, Long b) {
            this(new ByteArray(a.getPublic().getEncoded()), b);
        }
        public ContaValor(ByteArray a, Long b) {
            super(a, b);
        }
    }

    public static class Input implements Writable {
        public final byte[] transactionHash;
        public final int    outputIndex;

        public Input(byte[] transactionHash, int outputIndex) {
            this.transactionHash = transactionHash;
            this.outputIndex = outputIndex;
        }

        static Input read(DataInputStream dis) {
            try {
                byte[] auxTransactionHash = dis.readNBytes(HASH_SIZE);
                int    auxOutputIndex     = dis.readUnsignedByte();
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
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
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

    public int getCurrency() {
        return currency;
    }

    public List<Input> getInputs() {
        return inputs;
    }

    public List<? extends Output> getOutputs() {
        return outputs;
    }

    public static class Output implements Writable {
        public final int  receiverAccountIndex;
        public final long value;

        public Output(int receiverAccountIndex, long value) {
            //@formatter:off
            if (receiverAccountIndex < 0 || receiverAccountIndex > 255)
                throw new IllegalArgumentException();
            if (value < 0)
                throw new IllegalArgumentException();
            //@formatter:on
            this.receiverAccountIndex = receiverAccountIndex;
            this.value = value;
        }

        static Output read(DataInputStream dis) {
            try {
                int  auxReceiverIndex = dis.readUnsignedByte();
                long auxValue         = dis.readLong();
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

