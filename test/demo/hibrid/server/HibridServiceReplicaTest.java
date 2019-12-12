package demo.hibrid.server;

import demo.hibrid.request.Command;
import demo.hibrid.server.graph.COSManager;
import demo.hibrid.server.graph.ConflictDefinitionDefault;
import demo.hibrid.server.scheduler.EarlyScheduler;
import demo.hibrid.server.scheduler.LateScheduler;
import demo.hibrid.server.scheduler.QueuesManager;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class HibridServiceReplicaTest implements HibridReplier {
    QueuesManager queuesManager = new QueuesManager(2, 5);
    COSManager cosManager = new COSManager(2, 5, new ConflictDefinitionDefault());
    HibridExecutor executor = new HibridExecutor(10, 10);
    EarlyScheduler earlyScheduler = new EarlyScheduler(queuesManager);
    LateScheduler lateScheduler0 = new LateScheduler(cosManager, queuesManager, 0);
    LateScheduler lateScheduler1 = new LateScheduler(cosManager, queuesManager, 1);
    HibridWorker worker0 = new HibridWorker(0, 0, 0, cosManager, executor, this);
    HibridWorker worker1 = new HibridWorker(0, 1, 1, cosManager, executor, this);

    int TEST_SIZE = 100;
    Queue<ServerCommand> completedCommands = new ConcurrentLinkedQueue<>();

    public HibridServiceReplicaTest() throws InterruptedException {
        start(lateScheduler0, lateScheduler1, worker0, worker1);
        sendCommands(earlyScheduler);
    }

    private void sendCommands(EarlyScheduler earlyScheduler) throws InterruptedException {
        var commands = new Command[TEST_SIZE];
        for (int i = 0; i < commands.length; i+=2) {
            commands[i] = new Command(Command.GET, new int[]{0,1}, 1, 2, 3);
            commands[i+1] = new Command(Command.GET, new int[]{0,1}, 1, 2, 3);
        }
        System.out.println("Enviando " + commands.length + " comandos: " + Arrays.toString(commands));
        earlyScheduler.schedule(0, commands);
    }

    private void start(LateScheduler lateScheduler0, LateScheduler lateScheduler1, HibridWorker worker0, HibridWorker worker1) {
        lateScheduler0.start();
        lateScheduler1.start();
        worker0.start();
        worker1.start();
    }

    public static void main(String[] args) throws InterruptedException {
        var x = new HibridServiceReplicaTest();
    }

    @Override
    public void manageReply(ServerCommand serverCommand, boolean[] results) {
        completedCommands.add(serverCommand);
        System.out.println("---------------------");
        System.out.println(Thread.currentThread().getName()+"says: "+serverCommand);
        System.out.println(Thread.currentThread().getName()+"says: "+queuesManager);
        System.out.println(Thread.currentThread().getName()+"says: "+cosManager);
        System.out.println(Thread.currentThread().getName()+"says: "+"Resultados { size=" + completedCommands.size() + ", " + completedCommands + "}");
    }
}
