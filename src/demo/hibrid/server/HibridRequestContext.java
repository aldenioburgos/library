package demo.hibrid.server;

import bftsmart.tom.core.messages.TOMMessage;
import demo.hibrid.request.CommandResult;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class HibridRequestContext {

    public final TOMMessage request;
    private CommandResult[] results;
    private AtomicInteger pos = new AtomicInteger(0);

    public HibridRequestContext(int size, TOMMessage request) {
        this.results = new CommandResult[size];
        this.request = request;
    }

    public void add(CommandResult result) {
        this.results[pos.getAndAdd(1)] = result;
    }

    public boolean finished(){
        return pos.get() == results.length;
    }

    public CommandResult[] getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "HibridRequestContext{" +
                "request=" + request +
                ", results=" + Arrays.toString(results) +
                ", pos=" + pos +
                '}';
    }
}
