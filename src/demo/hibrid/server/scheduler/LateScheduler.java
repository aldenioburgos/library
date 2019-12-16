package demo.hibrid.server.scheduler;

import demo.hibrid.server.ServerCommand;
import demo.hibrid.server.graph.COSManager;
import demo.hibrid.stats.Stats;

import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;

public class LateScheduler extends Thread {

    private COSManager cosManager;
    private QueuesManager queuesManager;
    private int myPartition;
    private int id;

    public LateScheduler(COSManager cosManager, QueuesManager queuesManager, int id, int myPartition) {
        super("LateScheduler[" + id + "]");
        this.cosManager = cosManager;
        this.queuesManager = queuesManager;
        this.myPartition = myPartition;
        this.id  =id;
    }

    public void schedule(ServerCommand command) throws BrokenBarrierException, InterruptedException {
        // todos os lateschedulers das partições desse comando tem que esperar nessa barreira
        // para haver inclusão coordenada no grafo de dependências.
        if (command.barrier != null) {
            command.barrier.await();
        }

        // Só o latescheduler escolhido faz a inclusão no COS, o resto espera na próxima barreira.
        if (myPartition == min(command.distinctPartitions)) {
            cosManager.addTo(myPartition, command);
        }

        // todos os lateschedulers das partições desse comando tem que esperar o scheduler escolhido
        // terminar de inserir o comando no grafo.
        if (command.barrier != null) {
            command.barrier.await();
        }

    }

    @Override
    public void run() {
        try {
            while (true) {
                var takeInit = System.nanoTime();
                var command = queuesManager.takeCommandFrom(myPartition);
                Stats.lateSchedulerInit(id, takeInit, command);
                schedule(command);
                Stats.lateSchedulerEnd(id, command);
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }


    private int min(int[] commandPartitions) {
        return Arrays.stream(commandPartitions).min().getAsInt();
    }

}
