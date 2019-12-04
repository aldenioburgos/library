package demo.hibrid.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class CommandResult implements Serializable {

    private int id;
    private int index;

    public CommandResult() {
    }

    public CommandResult(int id, int index) {
        this.id = id;
        this.index = index;
    }

    public int getId() {
        return id;
    }

    public int getIndex() {
        return index;
    }


    public CommandResult fromBytes(DataInputStream dis) throws IOException {
        this.id = dis.readInt();
        this.index = dis.readInt();
        return this;
    }

    public void toBytes(DataOutputStream dos) throws IOException {
        dos.writeInt(id);
        dos.writeInt(index);
    }
}
