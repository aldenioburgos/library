package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.util.ByteArray;
import demo.coin.util.ByteUtils;
import demo.coin.util.CryptoUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.util.*;

import static demo.coin.util.ByteUtils.readByteSizedList;


//  /-----------------header---------------------\-/------------------------body------------------------\
//   <op_type>   [<accounts>]         <signature>     <currency>  [<inputs>]            [<outputs>]
//   <1>         <1>(<91>..<23205>)    <71>            <1>        <1>(<33>..<8415>)     <1>(<10>..<2550>)
// total: 1      +1+(91..23205)        +71             +1         +1+(33..8415)         +1+(10..2550)       = 210.. 11132bytes
public class Exchange extends Transfer {

    public Exchange(byte[] bytes) {
        super(bytes);
    }

    public Exchange(KeyPair keyPair, int currency, Map<CoinOperation, Integer> inputs, List<ContaValorMoeda> outputs) {
        super(keyPair, currency, inputs, outputs);
    }

    @Override
    public int getClassId(Set<SortedSet<Integer>> allPossiblePartitionsArrangement) {
        SortedSet<Integer> partitions = new TreeSet<>(Set.of(currency));
        for (var output : (List<Output>) outputs) {
            partitions.add(output.currency);
        }
        return partitions.toString().hashCode();
    }

    @Override
    protected List<? extends Transfer.Output> convertToOutputs(List<? extends ContaValor> outputsGiven) {
        //@formatter:off
        if (!outputsGiven.stream().allMatch(it -> it instanceof ContaValorMoeda))                                           throw new IllegalArgumentException();
        if (outputsGiven.stream().map(it -> ((ContaValorMoeda) it).c).anyMatch(it -> it == null || it < 0 || it > 255))     throw new IllegalArgumentException();
        //@formatter:on
        List<Output>          convertedOutputs = new ArrayList<>(outputsGiven.size());
        List<ContaValorMoeda> outputsToConvert = (List<ContaValorMoeda>) outputsGiven;
        for (var output : outputsToConvert) {
            convertedOutputs.add(new Output(addAccount(output.a), output.b, output.c));
        }
        return convertedOutputs;
    }

    @Override
    protected void validateType(DataInputStream dis) throws IOException {
        int type = dis.readUnsignedByte();
        if (type != OP_TYPE.EXCHANGE.ordinal())
            throw new RuntimeException("Invalid Type! " + type);
    }

    @Override
    protected void loadDataFrom(DataInputStream dis) throws IOException {
        this.currency = dis.readUnsignedByte();
        this.inputs = readByteSizedList(dis, Input::read);
        this.outputs = readByteSizedList(dis, Exchange.Output::read);
    }

    @Override
    protected void validateOutputs(CoinGlobalState globalState) {
        super.validateOutputs(globalState);
        //@formatter:off
        if (((List<Output>) outputs).stream().map(it -> it.currency).anyMatch(it -> it < 0 || it > 255 || !globalState.isCurrency(it)))   throw new IllegalArgumentException();
        //@formatter:on
    }

    @Override
    protected OP_TYPE getOpType() {
        return OP_TYPE.EXCHANGE;
    }

    @Override
    public byte[] execute(CoinGlobalState globalState) {
        try {
            // a transação é valida?
            validate(globalState);

            // consumir os utxos de entrada.
            globalState.removeUtxos(currency, accounts.get(issuer), getInputsAsUtxoAddresses());

            // criar os utxos de saída.
            byte[] transactionHash = CryptoUtil.hash(toByteArray());
            for (int i = 0; i < outputs.size(); i++) {
                Exchange.Output output = (Exchange.Output) outputs.get(i);
                globalState.addUtxo(output.currency, accounts.get(output.receiverAccountIndex), transactionHash, i, output.value);
            }

            // responder
            return ok();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return fail();
        }
    }

    @Override
    public String toString() {
        return "Exchange{" +
                "accounts=" + accounts +
                ", issuer=" + issuer +
                ", signature=" + ByteUtils.convertToText(signature)   +
                ", currency=" + currency +
                ", inputs=" + inputs +
                ", outputs=" + outputs +
                '}';
    }

    public static class ContaValorMoeda extends ContaValor {
        public final Integer c;


        public ContaValorMoeda(KeyPair conta, long valor, Integer moeda) {
            super(conta, valor);
            this.c = moeda;
        }

        public ContaValorMoeda(ByteArray conta, long valor, Integer moeda) {
            super(conta, valor);
            this.c = moeda;
        }
    }

    public static class Output extends Transfer.Output {
        public final int currency;

        public Output(int receiverAccountIndex, long value, int currency) {
            super(receiverAccountIndex, value);
            //@formatter:off
            if (currency < 0 || currency > 255) throw new IllegalArgumentException();
            //@formatter:on
            this.currency = currency;
        }

        static Exchange.Output read(DataInputStream dis) {
            try {
                int  auxReceiver = dis.readUnsignedByte();
                long auxValue    = dis.readLong();
                byte auxCurrency = dis.readByte();
                return new Exchange.Output(auxReceiver, auxValue, auxCurrency);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void writeTo(DataOutputStream dos) {
            super.writeTo(dos);
            try {
                dos.writeByte(currency);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return "{" +
                    "receiver=" + receiverAccountIndex +
                    ", currency=" + currency +
                    ", value=" + value +
                    '}';
        }
    }
}
