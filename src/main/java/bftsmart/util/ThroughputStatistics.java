/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bftsmart.util;


import demo.coin.util.Pair;

import java.io.*;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * @author eduardo
 */
public class ThroughputStatistics {

    private final int period = 1000; //millis
    private final int interval = 120;
    private List<Pair<Long, Integer>>[] executions;
    private List<Pair<Long, Integer>> arrivals;
    private int[][] counters;
    private long startedAt = 0;
    private long stoppedAt = 0;

    private boolean started = false;
    private int now = 0;
    private PrintWriter pw;
    private String print;

    private int numT = 0;
    private int id;

    private String path = "";
    boolean isStopped = true;
    int fakenow = 0;
    private SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");

    public ThroughputStatistics(int id, int numThreads, String filePath, String print) {
        this.print = print;
        this.id = id;
        this.numT = numThreads;
        this.path = filePath;
        this.arrivals = new ArrayList<>(300000);
        initExecutions(numThreads);
        initCounters(numThreads);
        initPrintWriter(filePath);
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

    private void initExecutions(int numThreads) {
        this.executions = new ArrayList[numThreads];
        for (int i = 0; i < executions.length; i++) {
            this.executions[i] = new ArrayList<>(300000);
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
                executions[threadId].add(new Pair(System.currentTimeMillis(), requestId));
            } catch (ArrayIndexOutOfBoundsException ignore) {
            }
        }
    }

    public void computeArrivals(Integer requestId) {
        if (!isStopped) {
            try {
                arrivals.add(new Pair<>(System.currentTimeMillis(), requestId));
            } catch (ArrayIndexOutOfBoundsException ignore) {
            }
        }
    }


    public void start() {
        if (!started) {
            started = true;
            now = 0;
            (new Timer()).scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    fakenow++;
                    if (fakenow == 30) {
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
        printRequestJourney();
        printStartStop();
        printWorkerJourney();
        pw.flush();
    }

    private void printWorkerJourney() {
        pw.println("Worker Journey:"+path);
        pw.println("Worker ID\tProcessing Time\tElapsed Time");
        for (int i = 0; i < executions.length; i++) {
            var lastProcessingTime = executions[i].get(0).a;
            for (var execution : executions[i]) {
                pw.print(i);
                pw.print('\t');
                pw.print(format.format(new Date(execution.a)));
                pw.print('\t');
                pw.println(execution.a - lastProcessingTime);
                lastProcessingTime = execution.a;
            }
        }
    }

    private void printRequestJourney() {
        pw.println("Request Journey");
        pw.println("Message ID\tArrival Time\tWorker ID\tProcessing Time\tDelay");
        for (var arrival : arrivals) {
            var quemEquandoExecutou = getExecutionPairForId(arrival.b);
            pw.print(arrival.b);
            pw.print('\t');
            pw.print(format.format(new Date(arrival.a)));
            pw.print('\t');
            pw.print(quemEquandoExecutou.a);
            pw.print('\t');
            pw.print(format.format(new Date(quemEquandoExecutou.b)));
            pw.print('\t');
            pw.println(quemEquandoExecutou.b - arrival.a);
        }
    }

    private Pair<Integer, Long> getExecutionPairForId(Integer requestId) {
        for (int i = 0; i < executions.length; i++) {
            for (var execution : executions[i]) {
                if (execution.b.equals(requestId)) {
                    return new Pair<>(i, execution.a);
                }
            }
        }
        return new Pair<>(-1, -1L);
    }

    private void printStartStop() {
        var begin = new Date(startedAt);
        var end = new Date(stoppedAt);
        pw.print("Started at:" +  format.format(begin));
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
