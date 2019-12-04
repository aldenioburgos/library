package demo.account.hibrid.commands;

public class Credit extends AccountCommand {

    private Account account;
    private int value;

    public Credit() {
    }

    public Credit(Account  account, int value) {
        this.id = idGenerator.getAndAdd(1);
        this.account = account;
        this.value = value;
    }

    public Account getAccount() {
        return account;
    }

    public int getValue() {
        return value;
    }

    public int getPartition() {
        return account.getPartition();
    }
}
