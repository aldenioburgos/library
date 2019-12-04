package demo.hibrid.server;

public final class HibridListServerStarter {


    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: ... ListServer <server_id> <max_index> <num_partitions> <num_workers>");
            System.exit(-1);
        }
        int serverId = Integer.parseInt(args[0]);
        int maxIndex = Integer.parseInt(args[1]);
        int numPartitions = Integer.parseInt(args[2]);
        int numWorkers = Integer.parseInt(args[3]);
        // cria o executor
        var executor = new HibridExecutor(maxIndex);
        // cria a replica
        new HibridServiceReplica(serverId, executor, null, numPartitions, numWorkers);
    }

}
