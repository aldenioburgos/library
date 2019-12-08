package demo.hibrid.server;

public final class HibridListServerStarter {

    private static int i = 0;

    public static void main(String[] args) {
        try {
            int serverId = Integer.parseInt(args[i++]);
            int minTimeToExecuteRead = Integer.parseInt(args[i++]);
            int minTimeToExecuteWrite = Integer.parseInt(args[i++]);
            int numPartitions = Integer.parseInt(args[i++]);
            int maxQueueSize = Integer.parseInt(args[i++]);
            int numWorkers = Integer.parseInt(args[i++]);
            int maxCOSSize = Integer.parseInt(args[i++]);
            int numOperations = Integer.parseInt(args[i++]);
            int numOpPerRequest = Integer.parseInt(args[i++]);

            // Validação
            if (numOpPerRequest < 0 || numOperations < 0 || minTimeToExecuteRead < 0 || minTimeToExecuteWrite < 0 || numPartitions <= 0 || maxQueueSize <= 0 || numWorkers <= 0 || maxCOSSize <= 0) throw new IllegalArgumentException("Invalid negative argument!");
            if (numWorkers < numPartitions) throw new IllegalArgumentException("num_workers < num_partitions!");

            // cria o executor
            var executor = new HibridExecutor(minTimeToExecuteRead, minTimeToExecuteWrite);

            StatisticsCollector.createInstance(numPartitions, numOperations, numOpPerRequest);
            // cria a replica
            new HibridServiceReplica(serverId, executor, numPartitions, maxQueueSize, numWorkers, maxCOSSize);

        } catch (Exception e) {
            System.out.println("Usage: ... ListServer <server_id> <min_time_to_execute_read> <min_time_to_execute_write> <num_partitions> <max_queue_size> <num_workers> <max_COS_size> <num_operations> <num_operations_per_request>");
            System.exit(-1);
        }

    }

}
