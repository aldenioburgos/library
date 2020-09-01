package demo.hibrid.request;

import java.io.*;
import java.util.Arrays;

public class CommandResult implements Serializable {

    private int id;
    private boolean[] results;

    public CommandResult() {
    }

    public CommandResult(int id, boolean[] results) {
        this.id = id;
        this.results = results;
    }


    public CommandResult fromBytes(byte[] bytes) {
        try (var bais = new ByteArrayInputStream(bytes);
             var dis = new DataInputStream(bais)){
             return fromBytes(dis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public CommandResult fromBytes(DataInputStream dis) throws IOException {
        this.id = dis.readInt();
        this.results = new boolean[dis.readInt()];
        for (int i = 0; i < results.length; i++) {
            results[i] = dis.readBoolean();
        }
        return this;
    }

    public void toBytes(DataOutputStream dos) throws IOException {
        dos.writeInt(id);
        dos.writeInt(results.length);
        for (boolean result : results) {
            dos.writeBoolean(result);
        }
    }

    @Override
    public String toString() {
        return "CommandResult{" +
                "id=" + id +
                ", results=" + Arrays.toString(results) +
                '}';
    }
}
