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
package demo.account.hibrid;


import demo.account.hibrid.commands.AccountCommand;
import demo.account.hibrid.commands.Transfer;

import java.io.IOException;
import java.util.*;

/**
 * Example client
 */
public class AccountClientHibrid {
    private Map<Integer, List<AccountCommand>> sentOperations = new HashMap<>();
    private boolean stop = false;
    private final int clientProcessId;
    private final int numThreads;
    private final int numRequests;
    private final int interval;
    private final int percWrites;
    private final int maxListIndex;
    private final int numOperationsPerRequest;
    private final int numPartitions;
    private final int[] percentualDistributionOfOperationsAmongPartition;
    private final int[] percentualOfPartitionsEnvolvedPerOperation;

    public AccountClientHibrid(int clientProcessId,
                               int numThreads,
                               int numRequests,
                               int interval,
                               int percWrites,
                               int maxListIndex,
                               int numOperationsPerRequest,
                               int numPartitions,
                               int[] percentualDistributionOfOperationsAmongPartition,
                               int[] percentualOfPartitionsEnvolvedPerOperation) {
        this.clientProcessId = clientProcessId;
        this.numThreads = numThreads;
        this.numRequests = numRequests;
        this.interval = interval;
        this.percWrites = percWrites;
        this.maxListIndex = maxListIndex;
        this.numOperationsPerRequest = numOperationsPerRequest;
        this.numPartitions = numPartitions;
        this.percentualDistributionOfOperationsAmongPartition = pileValues(percentualDistributionOfOperationsAmongPartition);
        this.percentualOfPartitionsEnvolvedPerOperation = pileValues(percentualOfPartitionsEnvolvedPerOperation);
        // start client
        try {
            this.start();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int[] pileValues(int[] origin) {
        int[] array = Arrays.copyOf(origin, origin.length);
        for (int i = 1; i < array.length; i++) {
            array[i] = array[i - 1] + array[i];
        }
        return array;
    }

    private void start() throws InterruptedException, IOException {
        AccountClientWorker[] workers = new AccountClientWorker[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Thread.sleep(100);
            System.out.println("Launching client " + (clientProcessId + i));
            workers[i] = new AccountClientWorker(clientProcessId + i, calcNumRequestsForWorker(i));
        }
        Thread.sleep(300);
        for (AccountClientWorker worker : workers) {
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


    public class AccountClientWorker extends Thread {
        private final int numberOfReqs;
        private final int id;
        private final BFTAccountHibrid server;
        private final Random rand = new Random();

        public AccountClientWorker(int id, int numberOfRqs) throws IOException {
            super("Client Worker " + id);
            this.id = id;
            this.numberOfReqs = numberOfRqs;
            this.server = new BFTAccountHibrid(id);
        }

        @Override
        public void run() {
            System.out.println("Executing experiment for " + numberOfReqs + " ops");


            for (int i = 0; i < numberOfReqs && !stop; i++) {
                if (isWriteOp()) {
                    int[] accountsSelected = selectAccounts();
                    int ammount = defineAmmount(accountsSelected[0]);
                    var transfer = createTransfer(accountsSelected, ammount);
                    var response = server.execute(transfer);
                } else {
                    int[] partitionsSelected = selectPartitions(1);
                    int[] accountsSelected = selectAccounts(partitionsSelected);

                }

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

        private boolean isWriteOp() {
            return (rand.nextInt(101) <= percWrites);
        }

        private Transfer createTransfer(int[] accountsEnvolved, int ammount) {
            return new Transfer(accountsEnvolved[0], ammount, Arrays.copyOfRange(accountsEnvolved, 1, accountsEnvolved.length));
        }

        private int defineAmmount(int accountToDebit) {
            return 1;
        }

        private int[] selectAccounts() {
            int numPartitionsEnvolved = numPartitionsEnvolved();
            int[] partitionsSelected = selectPartitions(numPartitionsEnvolved);

        }

        private int[] selectPartitions(int numPartitionsEnvolved) {
            var partitions = new int[numPartitionsEnvolved];
            for (int i = 0; i < partitions.length i++) {
                partitions[i] = selectPartition();
            }
        }

        private int selectPartition() {
            var selector = rand.nextInt(101);
            for (int i = 0; i < percentualDistributionOfOperationsAmongPartition.length; i++) {
                if (selector <= percentualDistributionOfOperationsAmongPartition[i]) {
                    return i;
                }
            }
            return 0;
        }

        private int numPartitionsEnvolved() {
            var selector = rand.nextInt(101);
            for (int i = 0; i < percentualOfPartitionsEnvolvedPerOperation.length; i++) {
                if (selector <= percentualOfPartitionsEnvolvedPerOperation[i]) {
                    return i;
                }
            }
            return 0;
        }
    }

}
