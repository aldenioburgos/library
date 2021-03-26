package demo.coin.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {

    public static <T> Set<Set<T>> groupBy(Set<T> items, int size) {
        var response = new HashSet<Set<T>>((items.size() / size) + 1);
        Set<T> part = new HashSet<>(size);
        int count = 0;
        for (T item : items) {
            part.add(item);
            count++;
            if (count == size) {
                response.add(part);
                part = new HashSet<>(size);
                count = 0;
            }
        }
        if (count > 0) {
            response.add(part);
        }
        return response;
    }

    public static <T> List<List<T>> groupBy(List<T> items, int size) {
        var response = new ArrayList<List<T>>((items.size() / size) + 1);
        List<T> part = new ArrayList<>(size);
        int count = 0;
        for (T item : items) {
            part.add(item);
            count++;
            if (count == size) {
                response.add(part);
                part = new ArrayList<>(size);
                count = 0;
            }
        }
        if (count > 0) {
            response.add(part);
        }
        return response;
    }

}
