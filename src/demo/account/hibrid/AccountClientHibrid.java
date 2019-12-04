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


import demo.account.hibrid.commands.Account;
import demo.account.hibrid.commands.AccountCommand;
import demo.account.hibrid.commands.CheckBalance;
import demo.account.hibrid.commands.Transfer;

import java.io.IOException;
import java.util.*;

/**
 * Example client
 */
public class AccountClientHibrid {
    private boolean stop = false;
    private final int clientProcessId;
    private final int numThreads;
    private final int numOperations;
    private final int interval;
    private final int percWrites;
    private final int maxListIndex;
    private final int numOperationsPerRequest;
    private final int numPartitions;
    private final int[] percentualDistributionOfOperationsAmongPartition;
    private final int[] percentualOfPartitionsEnvolvedPerOperation;

    public AccountClientHibrid(int clientProcessId,
                               int numThreads,
                               int numOperations,
                               int interval,
                               int percWrites,
                               int maxListIndex,
                               int numOperationsPerRequest,
                               int numPartitions,
                               int[] percentualDistributionOfOperationsAmongPartition,
                               int[] percentualOfPartitionsEnvolvedPerOperation) {
        this.clientProcessId = clientProcessId;
        this.numThreads = numThreads;
        this.numOperations = numOperations;
        this.numOperationsPerRequest = numOperationsPerRequest;
        this.interval = interval;
        this.percWrites = percWrites;
        this.maxListIndex = maxListIndex;
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
            array[i] = (array[i - 1] < 100) ? array[i - 1] + array[i] : 0;
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
        int exactDivision = numOperations / numThreads;
        int remainder = numOperations % numThreads;
        if (remainder == 0 || i != 0) {
            return exactDivision;
        }
        return exactDivision + remainder;
    }


    public class AccountClientWorker extends Thread {
        private final int numOperations;
        private final int id;
        private final BFTAccountHibrid server;
        private final Random rand = new Random();

        public AccountClientWorker(int id, int numberOfRqs) throws IOException {
            super("Client Worker " + id);
            this.id = id;
            this.numOperations = numberOfRqs;
            this.server = new BFTAccountHibrid(id);
        }

        @Override
        public void run() {
            System.out.println("Executing Client Worker "+id+" with " + numOperations + " ops.");
            for (int i = 0; i < numOperations && !stop; i =+ numOperationsPerRequest) {
                int numOpToExecute = Math.min(numOperations - i, numOperationsPerRequest);
                var operations = new AccountCommand[numOpToExecute];
                for (int j = 0; j < operations.length; j++) {
                    if (isWriteOp()) {
                        Account[] accountsSelected = selectAccounts();
                        operations[j] = createTransfer(accountsSelected, 1);
                    } else {
                        int selectedPartition = selectPartition();
                        Account selectedAccount = selectAccount(selectedPartition);
                        operations[j] = new CheckBalance(selectedAccount);
                    }
                }
                var responses = server.execute(operations);
                System.out.println("Client Worker "+id+": operations" + Arrays.deepToString(operations));
                System.out.println("Client Worker "+id+": responses " + Arrays.deepToString(responses));
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
            return (rand.nextInt(100) < percWrites);
        }

        private Transfer createTransfer(Account[] accountsEnvolved, int ammount) {
            return new Transfer(accountsEnvolved[0], ammount, Arrays.copyOfRange(accountsEnvolved, 1, accountsEnvolved.length));
        }

        private Account[] selectAccounts() {
            int numPartitionsEnvolved = numPartitionsEnvolved();
            var accounts = new Account[numPartitionsEnvolved];
            int[] partitionsSelected = selectPartitions(numPartitionsEnvolved);
            for (int i = 0; i < numPartitionsEnvolved; i++) {
                accounts[i] = selectAccount(partitionsSelected[i]);
            }
            return accounts;
        }

        private int numPartitionsEnvolved() {
            var selector = rand.nextInt(100);
            for (int i = 0; i < percentualOfPartitionsEnvolvedPerOperation.length; i++) {
                if (selector < percentualOfPartitionsEnvolvedPerOperation[i]) {
                    return i;
                }
            }
            return 0;
        }

        private int[] selectPartitions(int numPartitionsEnvolved) {
            var partitions = new int[numPartitionsEnvolved];
            for (int i = 0; i < partitions.length; i++) {
                partitions[i] = selectPartition();
            }
            return partitions;
        }

        private int selectPartition() {
            var selector = rand.nextInt(100);
            for (int i = 0; i < percentualDistributionOfOperationsAmongPartition.length; i++) {
                if (selector < percentualDistributionOfOperationsAmongPartition[i]) {
                    return i;
                }
            }
            return 0;
        }

        private Account selectAccount(int partition) {
            int division = maxListIndex / numPartitions;
            int remainder = maxListIndex % numPartitions;
            if (remainder == 0 || partition < (numPartitions - 1)) {
                return new Account(partition,rand.nextInt(division) + (partition * division));
            } else {
                return new Account(partition, rand.nextInt(division + remainder) + (partition * division));
            }
        }
    }

}
