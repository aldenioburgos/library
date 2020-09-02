package demo.hibrid.stats;

public class Stats {

    public static int earlyWorkers = 0;
    public static int lateWorkers = 0;
    public static int partitions = 0;
    public static long numOperations = 0;
    public static long start = 0;
    public static long end = 0;


    public static void print() {
        System.out.println("TP [partitions: "+partitions+", late workers: "+lateWorkers+"] :"+ throughput());
    }

    private static String throughput() {
        var nanos = end - start;
        var seconds = nanos / 1_000_000_000;
        var tp = numOperations/seconds;
        return String.valueOf(tp);
    }

}
