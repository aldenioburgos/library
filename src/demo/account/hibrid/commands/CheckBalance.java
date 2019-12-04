package demo.account.hibrid.commands;

public class CheckBalance extends AccountCommand {

    private Account account;

    public CheckBalance() {
    }

    public CheckBalance(Account account) {
        this.id = idGenerator.getAndAdd(1);
        this.account = account;
    }
}
