/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bftsmart.util;


import java.io.*;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;


/**
 * @author eduardo
 */
public class ThroughputStatistics {

    private int[][] counters;
    private int period = 1000; //millis

    private int interval = 120;

    private boolean started = false;
    private int now = 0;
    private PrintWriter pw;
    private String print;

    private int numT = 0;
    private int id;

    private String path = "";

    public ThroughputStatistics(int id, int numThreads, String filePath, String print) {
        this.print = print;
        this.id = id;
        numT = numThreads;
        this.path = filePath;
        counters = new int[numThreads][interval + 1];
        for (int i = 0; i < numThreads; i++) {
            for (int j = 0; j < interval + 1; j++) {
                counters[i][j] = 0;
            }
        }
        try {
            pw = new PrintWriter(new FileWriter(new File(filePath)));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
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
        pw.flush();
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
                        } else {
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
            System.out.println("Throughput: " + ret);
            return ret;
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return 0;
        }
    }

    public void printTP(long timeMillis) {
        int total = 0;
        for (int i = 0; i < numT; i++) {
            total = total + counters[i][now];
        }
        float tp = (float) (total * 1000 / (float) timeMillis);
        System.out.println("Throughput at " + print + " = " + tp + " operations/sec in sec : " + now);
    }

    boolean stoped = true;
    int fakenow = 0;

    public void start() {
        if (!started) {
            started = true;
            now = 0;
            (new Timer()).scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    fakenow++;
                    if (fakenow == 30) {
                        stoped = false;
                        for (int i = 0; i < numT; i++) {
                            counters[i][0] = 0;
                        }
                    } else if (!stoped) {

                        if (now <= interval) {
                            printTP(period);
                            now++;
                        }

                        if (now == interval + 1) {
                            stoped = true;
                            computeThroughput(period);
                        }
                    }
                }
            }, period, period);
        }
    }


    public void computeStatistics(int threadId, int amount) {
        if (!stoped) {
            try {
                counters[threadId][now] = counters[threadId][now] + amount;
            } catch (ArrayIndexOutOfBoundsException ignore) {
                //ignore.printStackTrace();
            }
        }
    }
}
