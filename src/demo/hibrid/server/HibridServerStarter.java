package demo.hibrid.server;

import demo.hibrid.stats.Stats;

public final class HibridServerStarter {

    private static int i = 0;

    public static void main(String[] args) {
        try {
            int serverId = Integer.parseInt(args[i++]);
            System.out.println("Server process id = " + serverId);
            int minTimeToExecuteRead = Integer.parseInt(args[i++]);
            System.out.println("Minimum time to execute each read operation = " + minTimeToExecuteRead+ " in milliseconds.");
            int minTimeToExecuteWrite = Integer.parseInt(args[i++]);
            System.out.println("Minimum time to execute each write operation = " + minTimeToExecuteWrite + " in milliseconds.");
            int numPartitions = Integer.parseInt(args[i++]);
            System.out.println("Number of partitions = " + numPartitions);
            int maxQueueSize = Integer.parseInt(args[i++]);
            System.out.println("Maximum partition queue size = " + maxQueueSize);
            int numWorkers = Integer.parseInt(args[i++]);
            System.out.println("Number of replica workers = " + numWorkers);
            int maxCOSSize = Integer.parseInt(args[i++]);
            System.out.println("Maximum size for each COS partition = " + maxCOSSize);
            int numOperations = Integer.parseInt(args[i++]);
            System.out.println("Total number of operations expected = " + numOperations + " // (for statistical reasons).");
            int numOpPerRequest = Integer.parseInt(args[i++]);
            System.out.println("Maximum number of operations per request = " + numOpPerRequest+ " // (for statistical reasons).");

            // Validação dos argumentos.
            if (numOpPerRequest < 0 || numOperations < 0 || minTimeToExecuteRead < 0 || minTimeToExecuteWrite < 0 || numPartitions <= 0 || maxQueueSize <= 0 || numWorkers <= 0 || maxCOSSize <= 0) throw new IllegalArgumentException("Invalid negative argument!");
            if (numWorkers < numPartitions) throw new IllegalArgumentException("num_workers < num_partitions!");

            // cria o executor
            var executor = new HibridExecutor(minTimeToExecuteRead, minTimeToExecuteWrite);

            // cria o gerenciador de estatísticas
            Stats.createInstance(numPartitions, numOperations, numOpPerRequest);

            // cria a replica
            new HibridServiceReplica(serverId, executor, numPartitions, maxQueueSize, numWorkers, maxCOSSize);

        } catch (Exception e) {
            System.out.println("Usage: ... ListServer <server_id> <min_time_to_execute_read> <min_time_to_execute_write> <num_partitions> <max_queue_size> <num_workers> <max_COS_size> <num_operations> <num_operations_per_request>");
            System.exit(-1);
        }

    }

}
