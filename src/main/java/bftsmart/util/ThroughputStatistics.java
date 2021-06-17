/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bftsmart.util;


import demo.coin.util.Pair;
import parallelism.ParallelServiceReplica;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;


/**
 * @author eduardo
 */
public class ThroughputStatistics {

    private final int period = 1000; //millis
    private final int interval = 120;
    private ParallelServiceReplica replica;


    private int[][] counters;
    private long startedAt = 0;
    private long stoppedAt = 0;

    private boolean started = false;
    private int now = 0;
    private PrintWriter pw;
    private final String print;

    private final int numT;
    private final int id;

//    List<Pair<Integer, Long>> arrival;
//    List<Pair<Integer, Long>> execution[];

    private String path = "";
    boolean isStopped = true;
    int fakenow = 0;
    private SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");

    public ThroughputStatistics(int id, int numThreads, String filePath, String print) {
        this.print = print;
        this.id = id;
        this.numT = numThreads;
        this.path = filePath;
        initCounters(numThreads);
        initPrintWriter(filePath);
//        arrival = new ArrayList<>(1000000);
//        execution = new ArrayList[numT];
//        for (int i = 0; i < numT; i++) {
//            execution[i] = new ArrayList<>(1000000);
//        }
    }

    public ThroughputStatistics(int id, int numThreads, String filePath, String print, ParallelServiceReplica replica) {
        this(id, numThreads, filePath, print);
        this.replica = replica;
    }

    private void initPrintWriter(String filePath) {
        try {
            this.pw = new PrintWriter(new FileWriter(filePath));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void initCounters(int numThreads) {
        this.counters = new int[numThreads][interval + 1];
        for (int i = 0; i < numThreads; i++) {
            for (int j = 0; j < interval + 1; j++) {
                counters[i][j] = 0;
            }
        }
    }


    public void computeStatistics(int threadId, int amount) {
        if (!isStopped) {
            try {
                counters[threadId][now] = counters[threadId][now] + amount;
            } catch (ArrayIndexOutOfBoundsException ignore) {
                //ignore.printStackTrace();
            }
        }
    }

    public void computeStatistics(int threadId, int amount, Integer requestId) {
        if (!isStopped) {
            try {
                counters[threadId][now] = counters[threadId][now] + amount;
//                execution[threadId].add(new Pair<>(requestId, System.currentTimeMillis()));
            } catch (ArrayIndexOutOfBoundsException ignore) {
            }
        }
    }

    public void computeArrivals(Integer requestId) {
        if (!isStopped) {
//            arrival.add(new Pair(requestId, System.currentTimeMillis()));
        }
    }


    public void start() {
        if (!started) {
            started = true;
            now = 0;
            (new Timer()).scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    fakenow++;
                    if (fakenow == 60) {
                        isStopped = false;
                        startedAt = System.currentTimeMillis();
                        for (int i = 0; i < numT; i++) {
                            counters[i][0] = 0;
                        }
                    } else if (!isStopped) {

                        if (now <= interval) {
                            printTP(period);
                            now++;
                        }

                        if (now == interval + 1) {
                            isStopped = true;
                            stoppedAt = System.currentTimeMillis();
                            replica.kill();
                            computeThroughput(period);
                            System.exit(0);
                        }
                    }
                }
            }, period, period);
        }
    }

    public void computeThroughput(long timeMillis) {
        for (int time = 0; time <= interval; time++) {
            int total = 0;
            for (int i = 0; i < numT; i++) {
                total = total + counters[i][time];
            }
            float tp = (float) (total * 1000 / (float) timeMillis);
            pw.println(time + " " + tp);
        }
        pw.flush();
        double tpAv = loadTP(this.path);
        pw.println("Average " + tpAv);
        printStartStop();
//        printRequestJourney();
//        pw.print("\n\n\n\n\n\n");
//        printWorkerJourney();
        pw.flush();
    }
//
//    private void printWorkerJourney() {
//        pw.println("Worker Journey:" + path);
//        for (int i = 0; i < numT; i++) {
//            printRequestLatencyStats(i);
//            printThreadPerformanceStats(i);
//        }
//    }
//
//    private void printRequestLatencyStats(int i) {
//        var tuple = calculateMinMaxMed(getStreamForThread(i).map(it -> it.executionTime - it.arrivalTime));
//        pw.println("Execution - Arrival in Thread[" + i + "]: num=" + tuple.counter + "reqs, min=" + tuple.min + "milisecs, med=" + tuple.med + "milisecs, max=" + tuple.max + "milisecs");
//    }
//
//    private void printThreadPerformanceStats(int i) {
//        var tuple = new Tuple();
//        getStreamForThread(i).sorted(Comparator.comparingLong(a -> a.executionTime)).reduce((a, b) -> {
//            var period = b.executionTime - a.executionTime;
//            tuple.counter++;
//            tuple.max = Long.max(period, tuple.max);
//            tuple.min = Long.min(period, tuple.min);
//            tuple.sum = Long.sum(period, tuple.sum);
//            return b;
//        });
//        tuple.med = tuple.sum / tuple.counter;
//        pw.println("Execution(x+1) - Execution(x) in Thread[" + i + "]: num=" + tuple.counter + "reqs, min=" + tuple.min + "milisecs, med=" + tuple.med + "milisecs, max=" + tuple.max + "milisecs");
//    }

    private Tuple calculateMinMaxMed(Stream<Long> numbers) {
        var tuple = new Tuple();
        numbers.forEach(it -> {
            tuple.max = Long.max(it, tuple.max);
            tuple.min = Long.min(it, tuple.min);
            tuple.sum = Long.sum(it, tuple.sum);
            tuple.counter++;
        });
        tuple.med = tuple.sum / tuple.counter;
        return tuple;
    }


    private class RequestJourney {
        long arrivalTime;
        long executionTime;
    }
//
//    private Stream<RequestJourney> getStreamForThread(int i) {
//        return execution[i].stream().map(it -> {
//            var journey = new RequestJourney();
//            arrival.stream().filter((Pair<Integer, Long> pair) -> pair.a.equals(it.a)).findFirst().ifPresent(pair -> journey.arrivalTime = pair.b);
//            journey.executionTime = it.b;
//            return (journey.executionTime == 0 || journey.arrivalTime == 0) ? null : journey;
//        }).filter(Objects::nonNull);
//    }

    private class Tuple {
        long min = Long.MAX_VALUE;
        long max = 0;
        long med = 0;
        long sum = 0;
        int counter = 0;
    }

//    private void printRequestJourney() {
//        var streamOfRequestLatencies = arrival.stream().map(reqArriv -> {
//            Integer requestId = reqArriv.a;
//            Long arrivalTime = reqArriv.b;
//            AtomicReference<Long> latency = new AtomicReference<>(0L);
//            for (int i = 0; i < numT && latency.get() == 0; i++) {
//                execution[i].stream().filter((Pair<Integer, Long> reqExec) -> reqExec.a.equals(requestId)).findFirst().ifPresent(it -> latency.set(it.b - arrivalTime));
//            }
//            return (latency.get() > 0 ? null : latency.get());
//        }).filter(Objects::nonNull);
//        var tuple = calculateMinMaxMed(streamOfRequestLatencies);
//        pw.println("Request Execution - Arrival: num=" + tuple.counter + "reqs, min=" + tuple.min + "milisecs, med=" + tuple.med + "milisecs, max=" + tuple.max + "milisecs");
//    }


    private void printStartStop() {
        var begin = new Date(startedAt);
        var end = new Date(stoppedAt);
        pw.print("Started at:" + format.format(begin));
        pw.print(" and Stopped at:" + format.format((end)));
        pw.println(" -> total: " + ((stoppedAt - startedAt) / 1000) + "secs and " + ((stoppedAt - startedAt) % 1000) + "millis.");
    }

    private double loadTP(String path) {
        try {
            FileReader fr = new FileReader(path);
            BufferedReader rd = new BufferedReader(fr);
            String line = null;
            int j = 0;
            LinkedList<Double> l = new LinkedList<Double>();
            int nextSec = 0;
            while (((line = rd.readLine()) != null)) {
                StringTokenizer st = new StringTokenizer(line, " ");
                try {
                    int i = Integer.parseInt(st.nextToken());
                    if (i <= 120) {
                        String t = st.nextToken();
                        double d = Double.parseDouble(t);
                        if (i > nextSec) {
                            for (int z = nextSec; z < i; z++) {
                                l.add(d);
                            }
                            nextSec = i;
                        }
                        if (i == nextSec) {
                            l.add(d);
                            nextSec++;
                        }
                    }
                } catch (Exception e) {
                }
            }
            fr.close();
            rd.close();
            double sum = 0;
            int i;
            for (i = 0; i < l.size(); i++) {
                sum = sum + l.get(i);
            }
            double ret = sum / l.size();
            return ret;
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return 0;
        }
    }

    private void printTP(long timeMillis) {
        int total = 0;
        for (int i = 0; i < numT; i++) {
            total = total + counters[i][now];
        }
        float tp = (float) (total * 1000 / (float) timeMillis);
        System.out.println("Throughput at " + print + " = " + tp + " operations/sec in sec : " + now);
    }
}
