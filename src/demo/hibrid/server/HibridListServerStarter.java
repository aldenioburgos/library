package demo.hibrid.server;

public final class HibridListServerStarter {

    private static int i = 0;

    public static void main(String[] args) {
        try {
            int serverId = Integer.parseInt(args[i++]);
            int maxIndex = Integer.parseInt(args[i++]);
            int numPartitions = Integer.parseInt(args[i++]);
            int maxQueueSize = Integer.parseInt(args[i++]);
            int numWorkers = Integer.parseInt(args[i++]);
            int maxCOSSize = Integer.parseInt(args[i++]);
            // Validação
            if (numWorkers < numPartitions) throw new IllegalArgumentException("num_workers < num_partitions!");
            if (maxIndex < 0 || numPartitions <=0 || maxQueueSize <= 0 || numWorkers <=0 || maxCOSSize <=0) throw new IllegalArgumentException("Invalid negative argument!");

            // cria o executor
            var executor = new HibridExecutor(maxIndex);
            // cria a replica
            new HibridServiceReplica(serverId, executor, numPartitions, maxQueueSize, numWorkers, maxCOSSize);

        } catch (Exception e) {
            System.out.println("Usage: ... ListServer <server_id> <max_index> <num_partitions> <max_queue_size> <num_workers> <max_COS_size>");
            System.exit(-1);
        }

    }

}
