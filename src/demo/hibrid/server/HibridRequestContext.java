package demo.hibrid.server;

import bftsmart.tom.core.messages.TOMMessage;
import demo.hibrid.request.CommandResult;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HibridRequestContext {

    public final TOMMessage message;
    private CommandResult[] results;
    private AtomicInteger pos = new AtomicInteger(0);
    private AtomicBoolean finished = new AtomicBoolean(false);

    public HibridRequestContext(int size, TOMMessage message) {
        assert size>0 : "The context size has to be greather than zero.";
        assert message!= null : "The messagen can't be null.";

        this.results = new CommandResult[size];
        this.message = message;
    }

    public void add(CommandResult result) {
        assert result != null : "A commmandResult can't be null.";
        this.results[pos.getAndAdd(1)] = result;
    }

    public boolean finished(){
        return pos.get() == results.length && finished.compareAndSet(false, true);
    }

    public CommandResult[] getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "HibridRequestContext{" +
                "request=" + message +
                ", results=" + Arrays.toString(results) +
                ", pos=" + pos +
                '}';
    }
}
