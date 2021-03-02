package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.core.UtxoAddress;
import demo.coin.util.CryptoUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static demo.coin.util.ByteUtils.readByteSizedList;


//  /-----------------header---------------------\-/------------------------body------------------------\
//   <op_type>   [<accounts>]         <signature>     <currency>  [<inputs>]            [<outputs>]
//   <1>         <1>(<91>..<23205>)    <71>            <1>        <1>(<33>..<8415>)     <1>(<10>..<2550>)
// total: 1      +1+(91..23205)        +71             +1         +1+(33..8415)         +1+(10..2550)       = 210.. 11132bytes
public class Exchange extends Transfer {

    public Exchange(byte[] bytes) {
        load(bytes);
    }

    public Exchange(byte[] issuer, List<Input> inputs, List<Output> outputs, byte currency) {
        super(issuer, inputs, outputs, currency);
    }


    @Override
    protected void validateType(DataInputStream dis) throws IOException {
        int type = dis.readUnsignedByte();
        if (type != OP_TYPE.EXCHANGE.ordinal()) throw new RuntimeException("Invalid Type! " + type);
    }

    @Override
    protected void loadDataFrom(DataInputStream dis) throws IOException {
        this.currency = dis.readUnsignedByte();
        this.inputs = readByteSizedList(dis, Input::read);
        this.outputs = readByteSizedList(dis, Exchange.Output::read);
    }

    @Override
    protected boolean isValidOutputs(CoinGlobalState globalState) {
        // a transação tem saídas? Os arrays tem o tamanho certo e as saídas tem valor positivo?
        if (outputs.size() <= 0) return false;

        for (Output output : (List<Output>) outputs) {
            if (output.receiverAccountIndex < 0 || output.receiverAccountIndex >= accounts.size()) return false;
            if (output.value <= 0) return false;
            if (!globalState.isCurrency(output.currency)) return false;
        }

        // pelo menos um recebedor tem que ser diferente do emitente
        if (outputs.stream().allMatch(it -> it.receiverAccountIndex == issuer)) return false;

        return true;
    }

    @Override
    protected OP_TYPE getOpType() {
        return OP_TYPE.EXCHANGE;
    }

    @Override
    public byte[] execute(CoinGlobalState globalState) {
        try {
            // a transação é valida?
            if (isInvalid(globalState)) {
                throw new IllegalArgumentException("Transação inválida <" + this + ">.");
            }

            // consumir os utxos de entrada.
            Set<UtxoAddress> utxoToConsume = inputs.stream()
                    .map(it -> new UtxoAddress(it.transactionHash, it.outputIndex))
                    .collect(Collectors.toSet());
            globalState.removeUtxos((byte) currency, accounts.get(issuer), utxoToConsume);

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
                ", signature=" + Arrays.toString(signature) +
                ", currency=" + currency +
                ", inputs=" + inputs +
                ", outputs=" + outputs +
                '}';
    }


    protected static class Output extends Transfer.Output {
        private final byte currency;

        public Output(int receiverAccountIndex, Long value, byte currency) {
            super(receiverAccountIndex, value);
            this.currency = currency;
        }

        static Exchange.Output read(DataInputStream dis) {
            try {
                int auxReceiver = dis.readUnsignedByte();
                long auxValue = dis.readLong();
                byte auxCurrency = dis.readByte();
                return new Exchange.Output(auxReceiver, auxValue, auxCurrency);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void write(DataOutputStream dos) {
            try {
                dos.writeByte(receiverAccountIndex);
                dos.writeLong(value);
                dos.writeByte(currency);
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
                    "receiver=" + receiverAccountIndex +
                    ", currency=" + currency +
                    ", value=" + value +
                    '}';
        }
    }


}
