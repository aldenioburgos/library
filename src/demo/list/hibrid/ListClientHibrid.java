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
package demo.list.hibrid;


import java.io.IOException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Example client
 */
public class ListClientHibrid {

    private boolean stop = false;
    private final int clientProcessId;
    private final int numThreads;
    private final int numRequests;
    private final int interval;
    private final int maxListIndex;
    private final int numOperationsPerRequest;
    private final int numPartitions;
    private final int[] percentualDistributionOfOperationsAmongPartition;
    private final int[] percentualDistributionOfWritesPerPartition;


    public ListClientHibrid(int clientProcessId,
                            int numThreads,
                            int numRequests,
                            int interval,
                            int maxListIndex,
                            int numOperationsPerRequest,
                            int numPartitions,
                            int[] percentualDistributionOfOperationsAmongPartition,
                            int[] percentualDistributionOfWritesPerPartition) {
        this.clientProcessId = clientProcessId;
        this.numThreads = numThreads;
        this.numRequests = numRequests;
        this.interval = interval;
        this.maxListIndex = maxListIndex;
        this.numOperationsPerRequest = numOperationsPerRequest;
        this.numPartitions = numPartitions;
        this.percentualDistributionOfOperationsAmongPartition = percentualDistributionOfOperationsAmongPartition;
        this.percentualDistributionOfWritesPerPartition = percentualDistributionOfWritesPerPartition;
    }

    public void start() throws InterruptedException, IOException {
        ListClientWorker[] workers = new ListClientWorker[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Thread.sleep(100);
            System.out.println("Launching client " + (clientProcessId + i));
            workers[i] = new ListClientWorker(clientProcessId + i, calcNumRequestsForWorker(i));
        }
        Thread.sleep(300);
        for (ListClientWorker worker : workers) {
            worker.start();
            worker.join();
        }
        (new Timer()).schedule(new TimerTask() {
            public void run() {
                stop = false;
            }
        }, 5 * 60000); //depois de 5 minutos
    }

    private int calcNumRequestsForWorker(int i) {
        int exactDivision = numRequests / numThreads;
        int remainder = numRequests % numThreads;
        if (remainder == 0 || i != 0) {
            return exactDivision;
        }
        return exactDivision + remainder;
    }


    public class ListClientWorker extends Thread {
        private int id;
        private int numberOfReqs;
        private BFTListHibrid<Integer> store;

        public ListClientWorker(int id, int numberOfRqs) throws IOException {
            super("Client Worker " + id);
            this.id = id;
            this.numberOfReqs = numberOfRqs;
            this.store = new BFTListHibrid<>(id);
        }

        @Override
        public void run() {
            System.out.println("Executing experiment for " + numberOfReqs + " ops");
            Random rand = new Random();

            for (int i = 0; i < numberOfReqs && !stop; i++) {
                int operationSelector = rand.nextInt(100);
                int indexToOperate = rand.nextInt(maxListIndex);

                //TODO continuar daqui.

                if (interval > 0) {
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            int i = 0;
            int clientProcessId = Integer.parseInt(args[i++]);
            int numThreads = Integer.parseInt(args[i++]);
            int numRequests = Integer.parseInt(args[i++]);
            int interval = Integer.parseInt(args[i++]);
            int maxListIndex = Integer.parseInt(args[i++]);
            int numOperationsPerRequest = Integer.parseInt(args[i++]);
            int numPartitions = Integer.parseInt(args[i++]);
            int[] percentualDistributionOfOperationsAmongPartition = new int[numPartitions];
            for (int j = 0; j < numPartitions; j++) {
                percentualDistributionOfOperationsAmongPartition[j] = Integer.parseInt(args[i++]);
            }
            int[] percentualDistributionOfWritesPerPartition = new int[numPartitions];
            for (int j = 0; j < numPartitions; j++) {
                percentualDistributionOfWritesPerPartition[i] = Integer.parseInt(args[i++]);
            }
            new ListClientHibrid(clientProcessId, numThreads, numRequests, interval, maxListIndex, numOperationsPerRequest, numPartitions, percentualDistributionOfOperationsAmongPartition, percentualDistributionOfWritesPerPartition).start();
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Usage: ... ListClientHibrid <process_id> <number_of_threads> <number_of_operations> <operations_per_request> <interval_beetween_requests> <max_list_index> <number_of_partitions> <% of op/partition> <% of writes/partition>");
            System.exit(-1);
        }
    }
}
