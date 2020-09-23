package demo.hibrid.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Command {
    public static final int ADD = 1;
    public static final int GET = 2;
    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    public final int id;
    public final int type;
    public final Integer[] partitions;
    public final Integer[] indexes;

    public Command(int id, int type, Integer[] partitions, Integer[] indexes) {
        assert type == ADD || type == GET : "Unkown command type: " + type;
        this.id = id;
        this.type = type;
        this.partitions = partitions;
        this.indexes = indexes;
    }

    public Command(int type, Integer[] partitions, Integer []indexes) {
        this(idGenerator.getAndAdd(1), type, partitions, indexes);
    }

    public Set<Integer> distinctPartitions(){
        return new HashSet<>(Arrays.asList(partitions));
    }

    public static Command fromBytes(DataInputStream dis) throws IOException {
        var id = dis.readInt();
        var type = dis.readInt();
        var partitions = new Integer[dis.readInt()];
        for (int i = 0; i < partitions.length; i++) {
            partitions[i] = dis.readInt();
        }
        var indexes = new Integer[dis.readInt()];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = dis.readInt();
        }
        return new Command(id, type, partitions, indexes);
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

    @Override
    public String toString() {
        return "{" +
//                "id=" +
                id +
//                ", type=" + type +
//                ", partitions=" + Arrays.toString(partitions) +
//                ", indexes=" + Arrays.toString(indexes) +
                '}';
    }
}
