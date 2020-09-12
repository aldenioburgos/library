package demo.hibrid.server.graph;

import demo.hibrid.request.Command;

public class ConflictDefinitionDefault implements ConflictDefinition<LockFreeNode> {

    @Override
    public boolean isDependent(LockFreeNode newNode, LockFreeNode oldNode) {
        return newNode.command.type == Command.ADD || oldNode.command.type == Command.ADD;
    }
}
