package demo.coin.core.transactions;

import demo.coin.core.CoinGlobalState;
import demo.coin.core.UtxoAddress;
import demo.coin.util.ByteUtils;
import demo.coin.util.CryptoUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Exchange extends Transfer {

    public Exchange() {
    }

    public Exchange(byte[] issuer, List<Input> inputs, List<? extends Output> outputs, byte currency) {
        super(issuer, inputs, outputs, currency);
    }

    @Override
    protected void loadDataFrom(DataInputStream dis) throws IOException {
        this.currency = dis.readNBytes(dis.readByte())[0];
        this.inputs = ByteUtils.readList(dis, Input::read);
        this.outputs = ByteUtils.readList(dis, Exchange.Output::read);
    }

    @Override
    protected void writeDataTo(DataOutputStream dos) throws IOException {
        byte[] currencies = getCurrencies();
        dos.writeByte(currencies.length);
        dos.write(currencies);
        ByteUtils.writeList(dos, inputs);
        ByteUtils.writeList(dos, outputs);
    }

    private byte[] getCurrencies() {
        Set<Byte> outputCurrenciesSet = outputs.stream().map(it -> ((Output) it).currency).collect(Collectors.toSet());
        outputCurrenciesSet.remove(currency);
        byte[] currencies = new byte[outputCurrenciesSet.size() + 1];
        int i = 0;
        currencies[i] = currency;
        for (var item : outputCurrenciesSet) {
            currencies[++i] = item;
        }
        return currencies;
    }

    @Override
    protected boolean isValidOutputs(CoinGlobalState globalState) {
        // a transação tem saídas? Os arrays tem o tamanho certo e as saídas tem valor positivo?
        if (outputs.size() <= 0) return false;

        for (var output : (List<Output>) outputs) {
            if (output.receiverPubKey.length != ISSUER_SIZE) return false;
            if (!globalState.isUser(output.receiverPubKey)) return false;
            if (output.value <= 0) return false;
            if (!globalState.isCurrency(output.currency)) return false;
        }

        return true;
    }

    @Override
    protected OP_TYPE getOpType() {
        return OP_TYPE.EXCHANGE;
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
            Exchange.Output output = (Exchange.Output) outputs.get(i);
            globalState.addUtxo(output.receiverPubKey, CryptoUtil.hash(toByteArray()), (byte) i, output.value, output.currency);
        }
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
        private final byte currency;

        public Output(byte[] receiverPubKey, Long value, byte currency) {
            super(receiverPubKey, value);
            this.currency = currency;
        }

        static Exchange.Output read(DataInputStream dis) {
            try {
                var auxReceiver = dis.readNBytes(ISSUER_SIZE);
                var auxValue = dis.readLong();
                var auxCurrency = dis.readByte();
                return new Exchange.Output(auxReceiver, auxValue, auxCurrency);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void write(DataOutputStream dos) {
            try {
                dos.write(this.receiverPubKey);
                dos.writeLong(this.value);
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
            return "Output{" +
                    "receiverPubKey=" + Arrays.toString(receiverPubKey) +
                    ", currency=" + currency +
                    ", value=" + value +
                    '}';
        }
    }


}
