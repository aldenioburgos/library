package demo.hibrid.server.scheduler;

import demo.hibrid.request.Command;
import demo.hibrid.request.CommandResult;
import demo.hibrid.server.graph.ConflictDefinition;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Partition {
    private final Node<CommandPair> commands = new Node();
    private final ConflictDefinition<Command> conflict;


    public Partition(ConflictDefinition<Command> conflict) {
        this.conflict = conflict;
    }

    public List<CompletableFuture<CommandResult>> add(Command command, CompletableFuture<CommandResult> futureResult) {
        List<CompletableFuture<CommandResult>> dependentResults = new LinkedList<>();
        Node<CommandPair> aux = commands;
        while (aux.next != null) {
            if (aux.next.value.futureResult.isDone()) {
                aux.next = aux.next.next;
            }
            if (aux.value != null && conflict.isDependent(aux.value.command, command)) {
                dependentResults.add(aux.value.futureResult);
            }
            if (aux.next == null) {
                break;
            } else {
                aux = aux.next;
            }
        }
        aux.next = new Node<>(new CommandPair(command, futureResult));
        return dependentResults;
    }
}


class Node<T> {
    final T value;
    Node<T> next;

    public Node() {
        this.value = null;
        this.next = null;
    }

    public Node(T value) {
        this.value = value;
        this.next = null;
    }

    @Override
    public String toString() {
        return ((value == null) ? "{" : value.toString()) + ((value != null && next != null) ? ", " : "") + ((next == null) ? "}" : next.toString());
    }
}

class CommandPair {
    public final Command command;
    public final CompletableFuture<CommandResult> futureResult;

    public CommandPair(Command command, CompletableFuture<CommandResult> futureResult) {
        this.command = command;
        this.futureResult = futureResult;
    }

    @Override
    public String toString() {
        return "{" + command.id + ", " + Arrays.toString(command.distinctPartitions()) + ", " + futureResult.isDone() + "}";
    }
}
