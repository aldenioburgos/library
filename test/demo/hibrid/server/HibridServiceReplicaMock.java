package demo.hibrid.server;

import demo.hibrid.request.Response;

public class HibridServiceReplicaMock extends  HibridServiceReplica {

    private static final System.Logger logger =  System.getLogger(HibridServiceReplicaMock.class.getName()) ;

    public HibridServiceReplicaMock(int id, int queueSize, int cosSize, int numPartitions, int numWorkers, HibridExecutor executor) {
        super(id, queueSize, cosSize, numPartitions, numWorkers, executor);
    }

    @Override
    protected void reply(Response response) {
//        logger.log(System.Logger.Level.OFF, response);
        if (context.isEmpty()) {
            System.out.println("End:"+System.nanoTime());
            System.exit(0); //TODO remover esse código depois de debugado!
        }
    }



}
