package demo.account.hibrid.commands;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Transfer extends AccountCommand {

    private Debit debit;
    private Credit[] credits;

    public Transfer() {
    }

    public Transfer(Account from, int value, Account[] to) {
        this.id = idGenerator.getAndAdd(1);
        this.debit = new Debit(from, value * to.length);
        this.credits = new Credit[to.length];
        this.credits = Arrays.stream(to).map(it -> new Credit(it, value)).collect(Collectors.toList()).toArray(new Credit[0]);
    }

    public int[] getPartitions() {
        var partitions = new int[credits.length + 1];
        partitions[0] = debit.getPartition();
        for (int i = 0; i < credits.length; i++) {
            partitions[i + 1] = credits[i].getPartition();
        }
        return partitions;
    }
}
