package demo.hibrid.server;

import demo.hibrid.server.graph.COSManager;
import demo.hibrid.server.graph.ConflictDefinitionDefault;
import demo.hibrid.server.scheduler.EarlyScheduler;
import demo.hibrid.server.queue.QueuesManager;
import demo.hibrid.stats.Stats;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HibridServerStarter {
    /**
     * Esses são os parâmetros que o programa aceita e precisa.
     */
    public enum Parametro {
        id, minRead, maxRead, minWrite, maxWrite, numPartitions, numWorkers, queueSize, cosSize,
    }

    private static Map<Parametro, Integer> params = new HashMap<>();

    public static void main(String[] args) {
        readParams(Arrays.asList(args));
        assert validateParams();
        var id = params.get(Parametro.id);
        var minRead = params.get(Parametro.minRead);
        var maxRead = params.get(Parametro.maxRead);
        var minWrite = params.get(Parametro.minWrite);
        var maxWrite = params.get(Parametro.maxWrite);
        var numPartitions = params.get(Parametro.numPartitions);
        var queueSize = params.get(Parametro.queueSize);
        var cosSize = params.get(Parametro.cosSize);
        var numWorkers = params.get(Parametro.numWorkers);

        // cria o executor
        var executor = new HibridExecutor(minRead, maxRead, minWrite, maxWrite);

        // cria o gerenciador de estatísticas
        Stats.createInstance().start();
        var queuesManager = new QueuesManager(numPartitions, queueSize);
        var earlyScheduler = new EarlyScheduler(queuesManager);
        var cosManager = new COSManager(numPartitions, cosSize, new ConflictDefinitionDefault());

        var replica = new HibridServiceReplica(id, earlyScheduler, queuesManager, cosManager, executor, numPartitions, numWorkers);
        replica.start();
    }

    private static boolean validateParams() {
        assert params.get(Parametro.minRead) >= 0 : "Invalid Argument, minRead < 0";
        assert params.get(Parametro.maxRead) >= 0 : "Invalid Argument, maxRead < 0";
        assert params.get(Parametro.minWrite) >= 0 : "Invalid Argument, minWrite < 0";
        assert params.get(Parametro.maxWrite) >= 0 : "Invalid Argument, maxWrite < 0";
        assert params.get(Parametro.numPartitions) > 0 : "Invalid Argument, numPartitions <= 0";
        assert params.get(Parametro.queueSize) > 0 : "Invalid Argument, queueSize <= 0";
        assert params.get(Parametro.cosSize) > 0 : "Invalid Argument, cosSize <= 0";
        assert params.get(Parametro.numWorkers) > 0 : "Invalid Argument, numWorkers <= 0";
        return true;
    }

    private static void readParams(List<String> args) {
        for (Parametro p : Parametro.values()) {
            params.put(p, get(args, p));
        }
    }

    private static int get(List<String> args, Parametro param) {
        var argument = args.stream().filter(it -> it.startsWith(param.toString())).findFirst();
        if (argument.isEmpty()) throw new RuntimeException("Parametro " + param.toString() + " não foi encontrado");
        var value = argument.get().substring(argument.get().indexOf('=') + 1);
        return Integer.parseInt(value);
    }


}
