package demo.hibrid.stats;

import java.util.ArrayList;
import java.util.List;

public class LateSchedulerStat {
    public final int id;
    public final List<Long> takeInit;
    public final List<Long> takeEnd;

    public LateSchedulerStat(int id, int numOperartions) {
        this.id = id;
        takeInit = new ArrayList<>(numOperartions);
        takeEnd = new ArrayList<>(numOperartions);
    }
}