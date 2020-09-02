package demo.hibrid.server;

import demo.hibrid.request.Command;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;

import static demo.hibrid.request.Command.ADD;
import static demo.hibrid.request.Command.GET;

public class ListExecutor implements ExecutorInterface {

    private LinkedList<Integer>[] listas;


    public ListExecutor(int listSize, int numParticoes) {
        this.listas = new LinkedList[numParticoes];
        for (int i = 0; i < numParticoes; i++) {
            var lista = new LinkedList<Integer>();
            for (int j = 0; j < listSize; j++) {
                lista.add(j);
            }
            listas[i] = lista;
        }
    }

    public boolean[] execute(Command command) {
        boolean[] ret = new boolean[command.partitions.length];
        for (int i = 0; i < command.partitions.length; i++) {
            var lista = listas[command.partitions[i]];
            switch (command.type) {
                case ADD:
                    if (!lista.contains(command.indexes[i])) ret[i] = lista.add(command.indexes[i]);
                    break;
                case GET:
                    ret[i] = lista.contains(command.indexes[i]);
                    break;
                default:
                    throw new UnsupportedOperationException(String.valueOf(command.type));
            }
        }
        return ret;
    }

    @Override
    public String toString() {
        return "ListExecutor{" +
                "listas=" + Arrays.stream(listas).map( it ->  String.valueOf(it.size()) ).collect(Collectors.toList()) +
                '}';
    }
}
