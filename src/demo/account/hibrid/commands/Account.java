package demo.account.hibrid.commands;

import java.io.Serializable;

public class Account implements Serializable {

    private int id;
    private int partition;

    public Account(int partition, int id) {
        this.id = id;
        this.partition = partition;
    }

    public int getId() {
        return id;
    }

    public int getPartition() {
        return partition;
    }
}
