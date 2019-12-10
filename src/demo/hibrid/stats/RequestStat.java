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
    }

    public CommandStat getCommand(Integer commandId) {
        if (!commands.containsKey(commandId)) {
            commands.put(commandId, new CommandStat());
        }
        return commands.get(commandId);
    }
}
