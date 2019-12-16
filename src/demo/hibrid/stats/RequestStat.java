package demo.hibrid.stats;

import java.util.HashMap;
import java.util.Map;

public class RequestStat {

    public int id;
    public long arrivalTime;
    public long replyTime;
    public final Map<Integer, CommandStat> commands;

    RequestStat(int id, int numCommands) {
        this.id = id;
        this.commands = new HashMap<>(numCommands);
        this.arrivalTime = System.nanoTime();
    }

    public CommandStat getCommand(Integer commandId) {
        if (!commands.containsKey(commandId)) {
            commands.put(commandId, new CommandStat());
        }
        return commands.get(commandId);
    }

    public static String title(){
        return "RequestStatId\tarrivalTime\treplyTime\t"+CommandStat.title();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (var command : commands.values()) {
            builder.append(id);
            builder.append('\t');
            builder.append(arrivalTime);
            builder.append('\t');
            builder.append(replyTime);
            builder.append('\t');
            builder.append(command.toString());
            builder.append('\n');
        }
        return builder.toString();
    }
}
