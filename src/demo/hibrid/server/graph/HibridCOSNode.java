package demo.hibrid.server.graph;

import demo.hibrid.server.ServerCommand;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


//TODO voltar para o grafo do professor.

public class HibridCOSNode<T extends ServerCommand> { //TODO não precisa extender ServerCommand, isso é só para debug.
    public final T data;// the item kept in the graph LockFreeNode
    final HibridCOS<T> cos;

    private List<HibridCOSNode<T>> dependsOn = new LinkedList<>();
    private List<HibridCOSNode<T>> isDependencyOf = new LinkedList<>();
    private AtomicBoolean reservedAtomic = new AtomicBoolean(false);
    private AtomicBoolean removedAtomic = new AtomicBoolean(false);
    private AtomicBoolean readyAtomic = new AtomicBoolean(false);

    HibridCOSNode(T data, HibridCOS<T> cos) {
        this.data = data;
        this.cos = cos;
    }

    void dependsOn(HibridCOSNode<T> oldNode) {
        assert Thread.currentThread().getName().startsWith("LateScheduler") : "COSNode.dependsOn() foi chamado pela thread " + Thread.currentThread().getName();
        assert !this.dependsOn.contains(oldNode) : "Tentou adicionar o mesmo nó duas vezes nas dependências de outro nó.";
        assert !oldNode.isDependencyOf.contains(this) : "Tentou adicionar o mesmo nó duas vezes nas dependências de outro nó.";
        assert this != oldNode : "Tentou criar dependência entre um nó e ele mesmo.";

        if (!oldNode.removedAtomic.get()) {
            this.dependsOn.add(oldNode);
            oldNode.isDependencyOf.add(this);
        }
    }

    void releaseDependentNodes() {
        assert Thread.currentThread().getName().startsWith("HibridServiceReplicaWorker") : "COSNode.releaseDependentNodes() foi chamado pela thread " + Thread.currentThread().getName();
        assert this.removedAtomic.get() : "Tentativa de liberar dependências de um nó que não foi excluído.";
        for (var node : this.isDependencyOf) {
            node.removeDependency(this);
        }
        isDependencyOf.clear();
    }

    private  void removeDependency(HibridCOSNode<T> node) {
        assert this.dependsOn.contains(node) : "Tentou remover uma dependência que não existe.";
        assert !this.readyAtomic.get() : "Tentou remover uma dependência de um nó já marcado como ready.";
        assert node != this : "Tentou remover a si mesmo de suas dependências.";

        this.dependsOn.remove(node);
        if (this.dependsOn.isEmpty() && this.readyAtomic.compareAndSet(false, true)) {
            cos.release(1);
        }
    }

    void checkIfReady() {
        if (this.dependsOn.isEmpty() && this.readyAtomic.compareAndSet(false, true)) {
            cos.release(1);
        }
    }

    void markAsRemoved() {
        assert Thread.currentThread().getName().startsWith("HibridServiceReplicaWorker") : "COSNode.WorkIsDone() foi chamado pela thread " + Thread.currentThread().getName();
        assert this.dependsOn.isEmpty() : "COSNode.WorkIsDone() foi chamado em um node que possui dependencias.";

        if (!this.removedAtomic.compareAndSet(false, true)) {
            throw new IllegalStateException("COSNode.WorkIsDone() foi chamado mais de uma vez.");
        }
    }

    boolean isDone() {
        return this.removedAtomic.get();
    }

    boolean tryReserve() {
        return this.readyAtomic.get() && this.reservedAtomic.compareAndSet(false, true);
    }

    @Override
    public String toString() { //TODO remover
        return "COSNode{" +
                "data=" + data +
                ", reservedAtomic=" + reservedAtomic +
                ", removedAtomic=" + removedAtomic +
                ", readyAtomic=" + readyAtomic +
                ", dependsOn=" + dependsOn.stream().map(it -> it.data.getCommandId()).collect(Collectors.toList()) +
                '}';
    }
}