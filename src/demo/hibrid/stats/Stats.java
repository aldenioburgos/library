package demo.hibrid.stats;

import java.util.concurrent.atomic.LongAdder;

public class Stats {

    public static int partitions = 0;
    public static long start = 0;
    public static long end = 0;
    public static LongAdder cosSize = new LongAdder();
    public static LongAdder queueSize = new LongAdder();
    public static int numOperations;

    public static boolean print() {
        System.out.println("Queue avg size: " + queueSize.doubleValue() / numOperations);
        System.out.println("COS avg size: " + cosSize.doubleValue() / numOperations);
        return true;
    }

    public static boolean queueSize(int size) {
        queueSize.add(size);
        return true;
    }

    public static boolean numOperations(int numOperations) {
        Stats.numOperations = numOperations;
        return true;
    }

    public static boolean cosSize(int size) {
        cosSize.add(size);
        return true;
    }


}
