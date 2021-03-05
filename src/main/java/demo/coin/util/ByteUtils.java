package demo.coin.util;

import io.netty.handler.codec.base64.Base64Encoder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

public class ByteUtils {

    public static byte[] convertToByteArray(String text){
        return Base64.getDecoder().decode(text);
    }

    public static String convertToText(byte[] bytes){
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static int[] byteArrayToIntArray(byte[] byteArray) {
        int[] array = new int[byteArray.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = byteArray[i] & 0xff;
        }
        return array;
    }

    public static byte[] intArrayToByteArray(int[] intArray) {
        byte[] array = new byte[intArray.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = (byte) intArray[i];
        }
        return array;
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(0, x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes, 0, Long.BYTES);
        buffer.flip();//need flip
        return buffer.getLong();
    }

    public static int readInt(byte[] bytes, int offset) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.put(bytes, offset, Integer.BYTES);
        buffer.flip();//need flip
        return buffer.getInt();
    }

    public static int readUnsignedShort(byte[] bytes, int offset) {
        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
        buffer.put(bytes, offset, Short.BYTES);
        buffer.flip();//need flip
        return buffer.getShort() & 0xffff;
    }

    public static int readUnsignedByte(byte[] buffer, int offset) {
        return (buffer[offset] & 0xff);
    }

    public static <T> List<T> readByteSizedList(DataInputStream dis, Function<DataInputStream, T> reader) throws IOException {
        int     size = dis.readUnsignedByte();
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(reader.apply(dis));
        }
        return list;
    }

    public static <T extends Writable> void writeByteSizedList(DataOutputStream dos, List<T> list) throws IOException {
        dos.writeByte(list.size());
        for (T item : list) {
            item.writeTo(dos);
        }
    }

    public static void writeBooleanArrayAsBytes(DataOutputStream dos, boolean[] booleanArray) throws IOException {
        int byteCounter = 0;
        int data        = 0;
        for (int i = 0; i < booleanArray.length; i++) {
            data = (data << 1) | (booleanArray[i] ? 1 : 0);
            if (++byteCounter == 8) {
                dos.writeByte(data);
                byteCounter = 0;
                data = 0;
            }
        }
        if (byteCounter > 0) {
            data <<= 8 - byteCounter;
            dos.writeByte(data);
        }
    }

    public static void readBooleanArrayFromBytes(DataInputStream dis, boolean[] booleanArray) throws IOException {
        int  byteCounter = 0;
        byte data        = dis.readByte();
        for (int i = 0; i < booleanArray.length; i++) {
            booleanArray[i] = (data ^ 128) > 0;
            data <<= 1;
            if (++byteCounter == 8 && (i + 1) < booleanArray.length) {
                data = dis.readByte();
            }
        }
    }


    public interface Writable {
        void writeTo(DataOutputStream dos);
    }
}
