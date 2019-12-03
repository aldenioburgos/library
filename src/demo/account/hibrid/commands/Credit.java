package demo.account.hibrid.commands;

public class Credit implements AccountCommand {

    private final int account;
    private final int value;

    public Credit(int account, int value) {
        this.account = account;
        this.value = value;
    }

    public int getSinteticValue(){
        return value;
    }
}
