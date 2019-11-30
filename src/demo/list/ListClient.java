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


import bftsmart.util.Storage;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Example client that updates a BFT replicated service (a counter).
 */
public class ListClient {

    public static int initId = 0;
    public static int op = BFTList.CONTAINS;
    public static boolean stop = false;

    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        if (args.length < 7) {
            System.out.println("Usage: ... ListClient <num. threads> <process id> <number of operations> <interval> <maxIndex> <verbose?> <parallel?>");
            System.exit(-1);
        }

        int numThreads = Integer.parseInt(args[0]);
        initId = Integer.parseInt(args[1]);

        int numberOfOps = Integer.parseInt(args[2]);
        int interval = Integer.parseInt(args[3]);
        int max = Integer.parseInt(args[4]);
        boolean verbose = Boolean.parseBoolean(args[5]);
        boolean parallel = Boolean.parseBoolean(args[6]);


        Client[] c = new Client[numThreads];

        for (int i = 0; i < numThreads; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(ListClient.class.getName()).log(Level.SEVERE, null, ex);
            }

            System.out.println("Launching client " + (initId + i));
            c[i] = new ListClient.Client(initId + i, numberOfOps, interval, max, verbose, parallel);
        }

        try {
            Thread.sleep(30000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ListClient.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (int i = 0; i < numThreads; i++) {
            c[i].start();
        }

        (new Timer()).scheduleAtFixedRate(new TimerTask() {
            public void run() {
                //change();
            }
        }, 60000, 60000); //a cada 1 minuto

        (new Timer()).schedule(new TimerTask() {
            public void run() {
                stop();
            }
        }, 5 * 60000); //depois de 5 minutos

        for (int i = 0; i < numThreads; i++) {
            try {
                c[i].join();
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
            }
        }
    }

    public static void stop() {
        stop = true;
    }

    public static void change() {
        op = (op == BFTList.CONTAINS)?BFTList.ADD:BFTList.CONTAINS;
    }

    static class Client extends Thread {
        int id;
        int numberOfOps;
        int interval;
        boolean verbose;
        BFTList<Integer> store;

        int maxIndex;

        public Client(int id, int numberOfOps, int interval, int maxIndex, boolean verbose, boolean parallel) {
            super("Client " + id);
            this.id = id;
            this.numberOfOps = numberOfOps;
            this.interval = interval;
            this.verbose = verbose;
            this.maxIndex = maxIndex;
            this.store = new BFTList<>(id, parallel);
        }


        public void run() {
            int req = 0;
            Storage st = new Storage(numberOfOps);

            System.out.println("Executing experiment for " + numberOfOps + " ops");

            for (int i = 0; i < numberOfOps && !stop; i++, req++) {
                if (i == 1) {
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ListClient.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                switch (op) {
                    case BFTList.ADD: {
                        int index = maxIndex - 1;
                        long last_send_instant = System.nanoTime();
                        store.add(index);
                        st.store(System.nanoTime() - last_send_instant);
                        break;
                    }
                    case BFTList.CONTAINS: {
                        int index = maxIndex - 1;
                        long last_send_instant = System.nanoTime();
                        store.contains(index);
                        st.store(System.nanoTime() - last_send_instant);
                        break;
                    }
                    case BFTList.GET: {
                        int index = maxIndex - 1;
                        long last_send_instant = System.nanoTime();
                        store.get(index);
                        st.store(System.nanoTime() - last_send_instant);
                        break;
                    }
                    case BFTList.REMOVE: {
                        int index = maxIndex - 1;
                        long last_send_instant = System.nanoTime();
                        store.remove(Integer.valueOf(index));
                        st.store(System.nanoTime() - last_send_instant);
                        break;
                    }
                    default: {
                        long last_send_instant = System.nanoTime();
                        store.size();
                        st.store(System.nanoTime() - last_send_instant);
                    }
                }

                if (verbose) {
                    System.out.println(this.id + " // sent!");
                }

                if (interval > 0 && i % 50 == 100) {
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException ex) {
                    }
                }

                if (verbose && (req % 1000 == 0)) {
                    System.out.println(this.id + " // " + req + " operations sent!");
                }

            }

            if (id == initId) {
                System.out.println(this.id + " // Average time for " + numberOfOps + " executions (-10%) = " + st.getAverage(true) / 1000 + " us ");
                System.out.println(this.id + " // Standard desviation for " + numberOfOps + " executions (-10%) = " + st.getDP(true) / 1000 + " us ");
                System.out.println(this.id + " // 90th percentile for " + numberOfOps + " executions = " + st.getPercentile(90) / 1000 + " us ");
                System.out.println(this.id + " // 95th percentile for " + numberOfOps + " executions = " + st.getPercentile(95) / 1000 + " us ");
                System.out.println(this.id + " // 99th percentile for " + numberOfOps + " executions = " + st.getPercentile(99) / 1000 + " us ");
            }

        }

    }
}
