package demo.hibrid.request;

import java.io.*;

public class Response {

    private int id;
    private CommandResult[] results;

    public Response() {
    }

    public Response(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public CommandResult[] getResults() {
        return results;
    }

    public byte[] toBytes() {
        try (var baos = new ByteArrayOutputStream();
             var dos = new DataOutputStream(baos)) {
            dos.writeInt(id);
            dos.writeInt(results.length);
            for (CommandResult result : results) {
                result.toBytes(dos);
            }
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Response fromBytes(byte[] bytes) {
        try (var bais = new ByteArrayInputStream(bytes);
             var dis = new DataInputStream(bais)) {
            this.id = dis.readInt();
            this.results = new CommandResult[dis.readInt()];
            for (int i = 0; i < results.length; i++) {
                this.results[i] = new CommandResult().fromBytes(dis);
            }
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
