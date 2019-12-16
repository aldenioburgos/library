package demo.hibrid.server;

import demo.hibrid.stats.Stats;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HibridServerStarter {


    public enum Parametro {
        id, minRead, minWrite, numPart, queuesize, numWorkers, cosSize, numSchedulers
    }

    private static Map<Parametro, Integer> params = new HashMap<>();

    public static void main(String[] args) {
        readParams(Arrays.asList(args));
        // cria o executor
        var executor = new HibridExecutor(params.get(Parametro.minRead), params.get(Parametro.minWrite));

        // cria o gerenciador de estatísticas
        Stats.createInstance();

        // cria a replica
        new HibridServiceReplica(
                params.get(Parametro.id),
                executor,
                params.get(Parametro.numPart),
                params.get(Parametro.queuesize),
                params.get(Parametro.numWorkers),
                params.get(Parametro.cosSize),
                params.get(Parametro.numSchedulers)
        );
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
