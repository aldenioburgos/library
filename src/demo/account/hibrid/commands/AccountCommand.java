package demo.account.hibrid.commands;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AccountCommand implements Serializable {

    protected static AtomicInteger idGenerator = new AtomicInteger(0);
    protected int id;


    public int getId() {
        return id;
    }
}
