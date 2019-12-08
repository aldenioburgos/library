package demo.hibrid.server.scheduler;

import demo.hibrid.server.ServerCommand;
import demo.hibrid.server.StatisticsCollector;
import demo.hibrid.server.graph.COSManager;

import java.util.concurrent.BrokenBarrierException;

public class LateScheduler extends Thread {

    private COSManager cosManager;
    private QueuesManager queuesManager;
    private int myPartition;
    private int id;


    public LateScheduler(COSManager cosManager, QueuesManager queuesManager, int myPartition) {
        super("LateScheduler[" + myPartition + "]");
        this.id = myPartition;
        this.cosManager = cosManager;
        this.queuesManager = queuesManager;
        this.myPartition = myPartition;
    }

    public void schedule(ServerCommand command) throws BrokenBarrierException, InterruptedException {
        // todos os lateschedulers das partições desse comando tem que esperar nessa barreira
        // para haver inclusão coordenada no grafo de dependências.
        command.awaitAtCyclicBarrierIfNeeded();

        // Só o latescheduler escolhido faz a inclusão no COS, o resto espera na próxima barreira.
        if (myPartition == min(command.getPartitions())) { //TODO aqui estou sempre escolhendo a thread de menor valor em myPartition, mas poderiamos fazer um roundrobin, por exemplo.
            cosManager.addTo(myPartition, command);
        }

        // todos os lateschedulers das partições desse comando tem que esperar o scheduler escolhido
        // terminar de inserir o comando no grafo.
        command.awaitAtCyclicBarrierIfNeeded();
    }

    @Override
    public void run() {
        try {
            while (true) {
                StatisticsCollector.getInstance().lateSchedulerStats[id].takeInit.add(System.currentTimeMillis());
                var command = queuesManager.takeCommandFrom(myPartition);
                StatisticsCollector.getInstance().lateSchedulerStats[id].takeEndScheduleInit.add(System.currentTimeMillis());
                schedule(command);
                StatisticsCollector.getInstance().lateSchedulerStats[id].scheduleEnd.add(System.currentTimeMillis());
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }


    private int min(int[] commandPartitions) {
        var min = Integer.MAX_VALUE;
        for (int i = 0; i < commandPartitions.length; i++) {
            min = Math.min(min, commandPartitions[i]);
        }
        return min;
    }

}
