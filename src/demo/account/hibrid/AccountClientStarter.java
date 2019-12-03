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


import java.util.Arrays;

/**
 * Example client
 */
public class AccountClientStarter {
    private static int i;

    public static void main(String[] args) throws Exception {
        try {
            i = 0;
            int clientProcessId = Integer.parseInt(args[i++]);
            int numThreads = Integer.parseInt(args[i++]);
            int numRequests = Integer.parseInt(args[i++]);
            int interval = Integer.parseInt(args[i++]);
            int maxListIndex = Integer.parseInt(args[i++]);
            int numOperationsPerRequest = Integer.parseInt(args[i++]);
            int percWrites = Integer.parseInt(args[i++]);
            int numPartitions = Integer.parseInt(args[i++]);
            // get array of args
            int[] percentualDistributionOfOperationsAmongPartition = getPercentualArrayOrArgs(args, numPartitions, "percentuais de distribuição das operações entre as partições");
            // get array of args
            int[] percentualOfPartitionsEnvolved = getPercentualArrayOrArgs(args, numPartitions, "percentuais de partições envolvidas nas operações");
            // create and run the client
            new AccountClientHibrid(clientProcessId, numThreads, numRequests, interval, maxListIndex, numOperationsPerRequest, percWrites, numPartitions, percentualDistributionOfOperationsAmongPartition, percentualOfPartitionsEnvolved);
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            System.out.println("Usage: ... AccountClientStarter <process_id> <number_of_threads> <number_of_operations> <operations_per_request> <interval_beetween_requests> <max_account_number> <%_of_writes> <number_of_partitions> <% of op/partition> <%_of_partitions_envolved>");
            System.out.println("ex 1: java demo.account.hibrid.AccountClientStarter 1 2 10000 50 5 100000 4 25 25 25 25 10 10 10 10 ");
            System.out.println("O exemplo 1 também pode ser escrito assim: java demo.account.hibrid.AccountClientStarter 1 2 10000 50 5 100000 4 25 ... 10 ...");
            System.out.println("ex 2: java demo.account.hibrid.AccountClientStarter 1 2 10000 50 5 100000 4 100 0 ...  20 ... ");
            e.printStackTrace();
            System.exit(-1);
        }
    }


    private static int[] getPercentualArrayOrArgs(String[] args, int numPartitions, String name) {
        int[] parcentualArrayOfArgs = new int[numPartitions];
        for (int j = 0; j < numPartitions; j++) {
            if (args[i].equals("...")) {
                if (j == 0) {
                    Arrays.fill(parcentualArrayOfArgs, 100 / numPartitions);
                } else if (j > 0) {
                    Arrays.fill(parcentualArrayOfArgs, j, numPartitions, Integer.parseInt(args[i - 1]));
                }
                i++;
                break;
            } else {
                parcentualArrayOfArgs[j] = Integer.parseInt(args[i++]);
            }
        }
        validatePercentualArrayOfArgs(parcentualArrayOfArgs, name);

        return parcentualArrayOfArgs;
    }

    private static void validatePercentualArrayOfArgs(int[] percentualArrayOfArgs, String name) {
        if (Arrays.stream(percentualArrayOfArgs).sum() != 100) {
            throw new IllegalArgumentException("A soma dos " + name + " tem que ser 100.");
        }
        if (Arrays.stream(percentualArrayOfArgs).anyMatch(it -> it < 0 || it > 100)) {
            throw new IllegalArgumentException("Os " + name + " devem ser números inteiros entre 0 e 100.");
        }
    }

}
