package demo.coin.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ByteUtils {

    private static ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

    public static byte[] longToBytes(long x) {
        buffer.putLong(0, x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        return buffer.getLong();
    }


    public static <T> List<T> readList(DataInputStream dis, Function<DataInputStream, T> reader) throws IOException {
        byte size = dis.readByte();
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(reader.apply(dis));
        }
        return list;
    }

    public static <T extends Writable> void writeList(DataOutputStream dos, List<T> list) throws IOException {
        dos.writeByte(list.size());
        for (T item : list) {
            item.writeTo(dos);
        }
    }

    public interface Writable {
        void writeTo(DataOutputStream dos);
    }
}
