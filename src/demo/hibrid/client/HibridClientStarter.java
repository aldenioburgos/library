/**
 * Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and
 * the authors indicated in the @author tags
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package demo.hibrid.client;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example client
 */
public class HibridClientStarter {
    private enum Parametro {
        id(true),
        numthreads(true),
        numOp(true),
        numOpPerReq(true),
        interval(true),
        maxServerIndex(true),
        numPartitions(true),
        distOpPart(false),
        percPart(false),
        percWrite(false);
        final boolean simple;

        Parametro(boolean simple) {
            this.simple = simple;
        }
    }

    private static Map<Parametro, Integer> params = new HashMap<>();
    private static Map<Parametro, int[]> arrParams = new HashMap<>();

    public static void main(String[] args) throws Exception {
        readParams(Arrays.asList(args));
        var config = new HibridClientConfig(
                params.get(Parametro.id),
                params.get(Parametro.numthreads),
                params.get(Parametro.numOp),
                params.get(Parametro.interval),
                params.get(Parametro.numOpPerReq),
                params.get(Parametro.numPartitions),
                arrParams.get(Parametro.distOpPart),
                arrParams.get(Parametro.percPart),
                arrParams.get(Parametro.percWrite)
        );

        // create and run the client
        var client = new HibridListClient(config, new ServerProxyFactory(ServerProxyHibridList.class));
        client.start();
    }

    private static void readParams(List<String> args) {
        for (Parametro p : Parametro.values()) {
            if (p.simple) {
                params.put(p, (Integer) get(args, p));
            } else {
                arrParams.put(p, (int[]) get(args, p));
            }
        }
    }

    private static Object get(List<String> args, Parametro param) {
        var argument = args.stream().filter(it -> it.startsWith(param.toString())).findFirst();
        if (argument.isEmpty()) throw new RuntimeException("Parametro " + param.toString() + " não foi encontrado");
        var value = argument.get().substring(argument.get().indexOf('=') + 1);
        if (param.simple) {
            return Integer.valueOf(value);
        } else {
            return getArray(param, value);
        }
    }

    private static int[] getArray(Parametro param, String value) {
        switch (param) {
            case distOpPart:
            case percPart:
                return getPercentualArrayOrArgs(value, params.get(Parametro.numPartitions), true);
            case percWrite:
                return getPercentualArrayOrArgs(value, params.get(Parametro.numPartitions), false);
            default:
                throw new IllegalArgumentException("Unknown (!simple) argument " + param);
        }
    }

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
