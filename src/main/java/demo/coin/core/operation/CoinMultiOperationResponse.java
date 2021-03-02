package demo.coin.core.operation;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CoinMultiOperationResponse {

    public byte[][] responses;
    public AtomicBoolean complete = new AtomicBoolean(false);

    public CoinMultiOperationResponse(int number) {
        responses = new byte[number][];
    }

    public CoinMultiOperationResponse(byte[] buffer) {
        try (var bais = new ByteArrayInputStream(buffer);
             var dis = new DataInputStream(bais)) {
            this.responses = new byte[dis.readUnsignedShort()][];
            for (int i = 0; i < this.responses.length; i++) {
                this.responses[i] = new byte[dis.readUnsignedShort()];
                dis.readFully(this.responses[i]);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public byte[] serialize() {
        try (var baos = new ByteArrayOutputStream();
             var dos = new DataOutputStream(baos)) {
            dos.writeShort(responses.length);
            for (int i = 0; i < responses.length; i++) {
                dos.writeShort(this.responses[i].length);
                dos.write(this.responses[i]);
            }
            dos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isComplete() {
        for (int i = 0; i < responses.length; i++) {
            if (responses[i] == null) return false;
        }
        return complete.compareAndSet(false, true);
    }

    public void add(int i, byte[] response) {
        this.responses[i] = response;
    }

}
