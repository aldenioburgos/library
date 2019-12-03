package demo.account.hibrid.commands;

import java.util.ArrayList;
import java.util.Arrays;

public class Transfer implements AccountCommand{

    private final Debit debit;
    private final Credit[] credits;

    public Transfer(int from, int value, int... to) {
        this.debit = new Debit(from, value * to.length);
        var auxCredits = new ArrayList<Credit>(to.length);
        Arrays.stream(to).forEach(it -> auxCredits.add(new Credit(it, value)));
        this.credits = auxCredits.toArray(new Credit[0]);
    }

    public int getSinteticValue(){
        return Math.abs(debit.getSinteticValue());
    }
}
