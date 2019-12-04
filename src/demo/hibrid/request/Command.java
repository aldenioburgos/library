package demo.hibrid.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class Command {
    public static final int ADD = 1;
    public static final int GET = 2;
    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    private int id;
    private int type;
    private int[] partitions;
    private int[] indexes;

    public Command() {
    }

    public Command(int type, int[] partition, int[] indexes) {
        if (type != ADD && type != GET) throw new IllegalArgumentException("Unkown command type: " + type);
        this.id = idGenerator.getAndAdd(1);
        this.type = type;
        this.partitions = partition;
        this.indexes = indexes;
    }

    public static AtomicInteger getIdGenerator() {
        return idGenerator;
    }

    public int getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public int[] getPartitions() {
        return partitions;
    }

    public int[] getIndexes() {
        return indexes;
    }

    public Command fromBytes(DataInputStream dis) throws IOException {
        this.id = dis.readInt();
        this.type = dis.readInt();
        this.partitions = new int[dis.readInt()];
        for (int i = 0; i < partitions.length; i++) {
            this.partitions[i] = dis.readInt();
        }
        this.indexes = new int[dis.readInt()];
        for (int i = 0; i < indexes.length; i++) {
            this.indexes[i] = dis.readInt();
        }
        return this;
    }

    public void toBytes(DataOutputStream dos) throws IOException {
        dos.writeInt(id);
        dos.writeInt(type);
        dos.writeInt(partitions.length);
        for (int partition : partitions) {
            dos.writeInt(partition);
        }
        dos.writeInt(indexes.length);
        for (int index : indexes) {
            dos.writeInt(index);
        }
    }
}
