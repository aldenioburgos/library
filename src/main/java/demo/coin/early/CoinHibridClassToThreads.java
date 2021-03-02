package demo.coin.early;

import demo.coin.core.operation.OperationContext;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptySortedSet;

/**
 *
 */
public class CoinHibridClassToThreads {

    public final Queue<OperationContext>[] queues;
    public final int[] tIds;

    private final boolean concurrent;
    private int threadIndex;

    public CoinHibridClassToThreads(int[] tIds, Queue<OperationContext>[] queues) {
        this.concurrent = (queues.length == 1);
        this.tIds = tIds;
        this.queues = queues;
    }

    public static Map<Integer, CoinHibridClassToThreads> generateMappings(int numberOfPartitions, Queue<OperationContext>[] allQueues) {
        Map<Integer, CoinHibridClassToThreads> mappings = new HashMap<>();
        var setOfPartitions = generateSortedSetOfPartitions(numberOfPartitions);
        var allArrangements = generateArrangement(emptySortedSet(), setOfPartitions);
        for (var arrangement : allArrangements) {
            List<Integer> partitions = new ArrayList<>(arrangement);
            var classId = partitions.toString().hashCode();
            var threadIds = partitions.stream().mapToInt(it->it).toArray();
            var queues = selectQueues(allQueues, partitions);
            mappings.put(classId, new CoinHibridClassToThreads(threadIds, queues));
        }
        return mappings;
    }

    private static Queue<OperationContext>[] selectQueues(Queue<OperationContext>[] allQueues, List<Integer> selectedPartitions) {
        List<Queue<OperationContext>> queues = new ArrayList<>(selectedPartitions.size());
        selectedPartitions.forEach(it -> queues.add(allQueues[it]));
        return queues.toArray(Queue[]::new);
    }

    private static SortedSet<Integer> generateSortedSetOfPartitions(int numberOfPartitions) {
        var set = new TreeSet<Integer>();
        for (int i = 0; i < numberOfPartitions; i++) {
            set.add(i);
        }
        return set;
    }

    private static Set<SortedSet<Integer>> generateArrangement(SortedSet<Integer> a, SortedSet<Integer> b) {
        Set<SortedSet<Integer>> arrangements = new HashSet<>();
        if (b.isEmpty()) {
            arrangements.add(a);
            return arrangements;
        }
        for (var element : b) {
            var elementsA = new TreeSet<>(a);
            var elementsB = new TreeSet<>(b);
            elementsB.remove(element);
            arrangements.addAll(generateArrangement(elementsA, elementsB)); // sem o element
            elementsA.add(element);
            arrangements.addAll(generateArrangement(elementsA, elementsB)); // com o element
        }
        return arrangements;
    }


    public int nextThreadIndex() {
        threadIndex = (threadIndex + 1) % tIds.length;
        return tIds[threadIndex];
    }

    public boolean isConcurrent() {
        return concurrent;
    }

    public static void main(String[] args) {
        Comparator<SortedSet<Integer>> comparator = (a, b) -> {         //comparator só para ficar bonito no output
            if (a.size() == b.size()) {
                var arrA = a.toArray(new Integer[]{});
                var arrB = b.toArray(new Integer[]{});
                var result = 0;
                for (int i = 0; i < a.size() && result == 0; i++) {
                    result = arrA[i].compareTo(arrB[i]);
                }
                return result;
            } else {
                return Integer.compare(a.size(), b.size());
            }
        };

        SortedSet<Integer> setToArrange = new TreeSet<>(Set.of(1, 2, 3, 4));
        var arrangement = generateArrangement(emptySortedSet(), setToArrange).stream().sorted(comparator).collect(Collectors.toList());
        System.out.println(arrangement);
    }


}
