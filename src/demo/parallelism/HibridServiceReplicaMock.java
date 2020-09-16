package demo.parallelism;

import demo.hibrid.request.Response;
import demo.hibrid.server.ExecutorInterface;
import demo.hibrid.server.HibridServiceReplica;
import demo.hibrid.stats.Stats;

public class HibridServiceReplicaMock extends HibridServiceReplica {

    public HibridServiceReplicaMock(int id, int queueSize, int cosSize, int numPartitions, int numWorkers, ExecutorInterface executor) {
        super(id, queueSize, cosSize, numPartitions, numWorkers, executor);
    }

    @Override
    public void reply(Response response) {
        if (context.isEmpty()) {
            Stats.end = System.nanoTime();
            System.exit(0);
        }
    }



}
