package demo.hibrid.client;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example client
 */
public class Main {

    private enum Parametro {
        id,
        numthreads,
        numOp,
        numOpPerReq,
        maxServerIndex,
        numPartitions,
        distOpPart(true),
        percPart(true),
        percWrite(true);
        final boolean isArray;

        Parametro(boolean isArray) {
            this.isArray = isArray;
        }

        Parametro() {
            this(false);
        }
    }

    private final static Map<Parametro, Integer> params = new HashMap<>();
    private final static Map<Parametro, int[]> arrParams = new HashMap<>();

    public static void main(String[] args) {
        readParams(Arrays.asList(args));
        var config = new HibridClientConfig(
                Integer.MAX_VALUE,
                params.get(Parametro.id),
                params.get(Parametro.numthreads),
                params.get(Parametro.numOp),
                params.get(Parametro.numOpPerReq),
                params.get(Parametro.numPartitions),
                arrParams.get(Parametro.distOpPart),
                arrParams.get(Parametro.percPart),
                arrParams.get(Parametro.percWrite)
        );

        // create and start the client
        var client = new HibridClient(config);
        client.start();
    }

    private static void readParams(List<String> args) {
        for (Parametro p : Parametro.values()) {
            if (p.isArray) {
                arrParams.put(p, (int[]) get(args, p));
            } else {
                params.put(p, (Integer) get(args, p));
            }
        }
    }

    private static Object get(List<String> args, Parametro param) {
        var argument = args.stream().filter(it -> it.startsWith(param.toString())).findFirst();
        if (argument.isEmpty()) throw new RuntimeException("Parametro " + param.toString() + " não foi encontrado");
        var value = argument.get().substring(argument.get().indexOf('=') + 1);
        if (param.isArray) {
            return getArray(param, value);
        } else {
            return Integer.valueOf(value);
        }
    }

    private static int[] getArray(Parametro param, String value) {
        return switch (param) {
            case distOpPart, percPart -> getPercentualArrayOrArgs(value, params.get(Parametro.numPartitions), true);
            case percWrite -> getPercentualArrayOrArgs(value, params.get(Parametro.numPartitions), false);
            default -> throw new IllegalArgumentException("Unknown (isArray) argument: " + param);
        };
    }

    @SuppressWarnings("ConstantConditions")
    private static int[] getPercentualArrayOrArgs(String value, Integer numPartitions, boolean aSomaEh100) {
        int[] parcentualArrayOfArgs = new int[numPartitions];
        String[] itens = value.split(",");
        for (int i = 0; i < numPartitions; i++) {
            if (itens[i].equals("...")) {
                if (i == 0 && aSomaEh100) {
                    Arrays.fill(parcentualArrayOfArgs, 100 / numPartitions);
                } else if (i > 0) {
                    Arrays.fill(parcentualArrayOfArgs, i, numPartitions, Integer.parseInt(itens[i - 1]));
                } else {
                    throw new IllegalArgumentException("Não é possível definir os valores do array");
                }
                break;
            } else {
                parcentualArrayOfArgs[i] = Integer.parseInt(itens[i]);
            }
        }
        return parcentualArrayOfArgs;
    }
}
