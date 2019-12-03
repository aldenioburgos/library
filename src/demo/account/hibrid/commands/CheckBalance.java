package demo.account.hibrid.commands;

public class CheckBalance implements AccountCommand {

    private final int account;
    private int balance;

    public CheckBalance(int account) {
        this.account = account;
    }

    public int getSinteticValue(){
        return balance;
    }
}
