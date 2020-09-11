package demo.util;

public class Utils {


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


    public static <T> void fillWith(T[] array, Creator<T> creator) {
        for (int i = 0; i < array.length; i++) {
            array[i] = creator.apply();
        }
    }

    public interface Creator<T> {
        T apply();
    }

    public interface Action<T> {
        void apply(T t);
    }

    public interface Filter<T> {
        boolean apply(T t);
    }

    public interface Fold<T> {
        void apply(T t1, T t2);
    }
}

