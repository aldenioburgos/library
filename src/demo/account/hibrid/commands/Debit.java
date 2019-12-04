package demo.account.hibrid.commands;

public class Debit extends AccountCommand {

    private Account account;
    private int value;

    public Debit() {
    }

    public Debit(Account account, int value) {
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

    int getPartition() {
        return account.getPartition();
    }
}
