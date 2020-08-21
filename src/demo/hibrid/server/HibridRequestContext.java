package demo.hibrid.server;

import demo.hibrid.request.CommandResult;
import demo.hibrid.request.Request;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HibridRequestContext {

    private final CommandResult[] results;
    private final AtomicInteger pos = new AtomicInteger(0);
    private final AtomicBoolean finished = new AtomicBoolean(false);

    public HibridRequestContext(Request request) {
        assert request.getCommands().length > 0 : "The context size has to be greather than zero.";

        this.results = new CommandResult[request.getCommands().length];
    }

    public void add(CommandResult result) {
        assert result != null : "A commmandResult can't be null.";
        this.results[pos.getAndAdd(1)] = result;
    }

    public boolean isRequestFinished() {
        return pos.get() == results.length && finished.compareAndSet(false, true);
    }

    public CommandResult[] getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "HibridRequestContext{" +
                ", results=" + Arrays.toString(results) +
                ", pos=" + pos +
                '}';
    }
}
