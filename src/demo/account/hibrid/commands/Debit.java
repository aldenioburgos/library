package demo.account.hibrid.commands;

public class Debit implements AccountCommand {

    private final int account;
    private final int value;

    public Debit(int account, int value) {
        this.account = account;
        this.value = value;
    }


    public int getSinteticValue() {
        return -value;
    }
}
