package demo.hibrid.request;

import java.io.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class Request {

    private static final AtomicInteger idGenerator = new AtomicInteger(0);
    private int id;
    private Command[] commands;

    public Request() {
    }

    public Request(int processId, int workerId, Command... commands) {
        var counter = idGenerator.getAndAdd(1);
        this.id = Objects.hash(processId, workerId, counter);
        this.commands = commands;
    }

    public byte[] toBytes() {
        try (var baos = new ByteArrayOutputStream();
             var dos = new DataOutputStream(baos)) {
            dos.writeInt(id);
            dos.writeInt(commands.length);
            for (Command command : commands) {
                command.toBytes(dos);
            }
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Request fromBytes(byte[] bytes) {
        try (var bais = new ByteArrayInputStream(bytes);
             var dis = new DataInputStream(bais)) {
            this.id = dis.readInt();
            this.commands = new Command[dis.readInt()];
            for (int i = 0; i < commands.length; i++) {
                this.commands[i] = Command.fromBytes(dis);
            }
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getId() {
        return id;
    }

    public Command[] getCommands() {
        return commands;
    }

    @Override
    public String toString() {
        return "Request{" +
                "id=" + id +
                ", commands=" + Arrays.deepToString(commands) +
                '}';
    }
}
