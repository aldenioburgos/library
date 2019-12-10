/**
 * Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and
 * the authors indicated in the @author tags
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package demo.list;

//import bftsmart.tom.parallelism.ParallelMapping;

import bftsmart.util.ExtStorage;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example client that updates a BFT replicated service (a counter).
 *
 */
public class ListClientMO {

    public int initId;
    public int p;
    public boolean stop = false;

    public ListClientMO(int numThreads, int numberOfReqs, int interval, int max, boolean verbose, boolean parallel, int numberOfOpsPerReq, int initId, int p) {
        this.p = p;
        this.initId = initId;
        System.out.println("percent : " + p);

        Client[] c = new Client[numThreads];

        for (int i = 0; i < numThreads; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(ListClientMO.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Creating client " + (this.initId + i));
            c[i] = new Client(this.initId + i, numberOfReqs, numberOfOpsPerReq, interval, max, verbose, parallel);
        }

        try {
            Thread.sleep(300);
        } catch (InterruptedException ex) {
            Logger.getLogger(ListClientMO.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (Client client : c) {
            client.start();
        }

        (new Timer()).schedule(new TimerTask() {
            public void run() {
                stop();
            }
        }, 5 * 60000); //depois de 5 minutos

        for (Client client : c) {
            try {
                client.join();
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
            }
        }
    }

    public void stop() {
        this.stop = true;
    }

    class Client extends Thread {
        int id;
        int numberOfReqs;
        int interval;
        boolean verbose;
        BFTListMO<Integer> store;
        int maxIndex;
        int opPerReq;

        public Client(int id, int numberOfRqs, int opPerReq, int interval, int maxIndex, boolean verbose, boolean parallel) {
            super("Client " + id);
            this.id = id;
            this.numberOfReqs = numberOfRqs;
            this.opPerReq = opPerReq;
            this.interval = interval;
            this.verbose = verbose;
            this.maxIndex = maxIndex;
            this.store = new BFTListMO<>(id, parallel);
        }

        public void run() {
            System.out.println(Thread.currentThread() + " is the ClientMO id="+id);

            ExtStorage sRead = new ExtStorage();
            ExtStorage sWrite = new ExtStorage();
            System.out.println("Executing experiment for " + numberOfReqs + " ops");
            Random rand = new Random();
            Random indexRand = new Random();

            int req = 0;
            for (int i = 0; i < numberOfReqs; i++, req++) {
                if (i == 1) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ListClientMO.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                int r = rand.nextInt(100);
                if (r < p) {
                    Integer[] reqs = new Integer[opPerReq];
                    for (int x = 0; x < reqs.length; x++) {
                        reqs[x] = indexRand.nextInt(maxIndex);
                    }
                    long last_send_instant = System.nanoTime();
                    store.add(reqs);
                    sWrite.store(System.nanoTime() - last_send_instant);
                } else {
                    Integer[] reqs = new Integer[opPerReq];
                    for (int x = 0; x < reqs.length; x++) {
                        reqs[x] = indexRand.nextInt(maxIndex);
                    }
                    long last_send_instant = System.nanoTime();
                    store.contains(reqs);
                    sRead.store(System.nanoTime() - last_send_instant);
                }

                if (verbose) {
                    System.out.println(this.id + " // sent!");
                }

                if (interval > 0 && i % 50 == 100) {
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ListClientMO.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                if (verbose && (req % 1000 == 0)) {
                    System.out.println(this.id + " // " + req + " operations sent!");
                }

            }

            if (id == initId) {
                System.out.println(this.id + " //READ Average time for " + numberOfReqs + " executions (-10%) = " + sRead.getAverage(true) / 1000 + " us ");
                System.out.println(this.id + " //READ Standard desviation for " + numberOfReqs + " executions (-10%) = " + sRead.getDP(true) / 1000 + " us ");
                System.out.println(this.id + " // READ 90th percentile for " + numberOfReqs + " executions = " + sRead.getPercentile(90) / 1000 + " us ");
                System.out.println(this.id + " // READ 95th percentile for " + numberOfReqs + " executions = " + sRead.getPercentile(95) / 1000 + " us ");
                System.out.println(this.id + " // READ 99th percentile for " + numberOfReqs + " executions = " + sRead.getPercentile(99) / 1000 + " us ");

                System.out.println(this.id + " //WRITE Average time for " + numberOfReqs + " executions (-10%) = " + sWrite.getAverage(true) / 1000 + " us ");
                System.out.println(this.id + " //WRITE Standard desviation for " + numberOfReqs + " executions (-10%) = " + sWrite.getDP(true) / 1000 + " us ");
                System.out.println(this.id + " // WRITE 90th percentile for " + numberOfReqs + " executions = " + sWrite.getPercentile(90) / 1000 + " us ");
                System.out.println(this.id + " // WRITE 95th percentile for " + numberOfReqs + " executions = " + sWrite.getPercentile(95) / 1000 + " us ");
                System.out.println(this.id + " // WRITE 99th percentile for " + numberOfReqs + " executions = " + sWrite.getPercentile(99) / 1000 + " us ");
            }

            System.out.println(this.id + " FINISHED!!!");

        }

    }



    public static void main(String[] args)  {
        if (args.length < 8) {
            System.out.println("Usage: ... ListClient <num. threads> <process id> <number of requests> <interval> <maxIndex> <parallel?> <operations per request> <conflict percent>");
            System.exit(-1);
        }

        int numThreads = Integer.parseInt(args[0]);
        int initId = Integer.parseInt(args[1]);
        int numberOfReqs = Integer.parseInt(args[2]);
        int interval = Integer.parseInt(args[3]);
        int max = Integer.parseInt(args[4]);
        boolean verbose = true;
        boolean parallel = Boolean.parseBoolean(args[5]);
        int numberOfOps = Integer.parseInt(args[6]);
        int p = Integer.parseInt(args[7]);
//        int numThreads = 50;
//        int initId = 5001;
//
//        int numberOfReqs = 100000;
//        int interval = 0;
//        int max = 10000;
//        boolean verbose = true;
//        boolean parallel = true;
//        int numberOfOps = 50;
//        int p = 10;

        new ListClientMO(numThreads, numberOfReqs, interval, max, verbose, parallel, numberOfOps, initId, p);
    }
}
