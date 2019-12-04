package demo.account.hibrid.commands;

import java.io.Serializable;

public class AccountResponse implements Serializable {

    private int id;
    private int balance;

    public AccountResponse() {
    }

    public AccountResponse(int id, int balance) {
        this.id = id;
        this.balance = balance;
    }

    public int getId() {
        return id;
    }

    public int getBalance() {
        return balance;
    }
}
