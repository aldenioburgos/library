package demo.hibrid.server.util;

public class ThreadUtil {


    public static Thread[] start(Thread... threads) {
        for (Thread thread : threads) {
            thread.start();
        }
        return threads;
    }


    public static void join(Thread... threads) {
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
