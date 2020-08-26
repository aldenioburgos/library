package demo.hibrid.server.graph;

import demo.hibrid.request.Command;
import demo.hibrid.server.CommandEnvelope;

public class ConflictDefinitionDefault implements ConflictDefinition<CommandEnvelope> {

    @Override
    public boolean isDependent(CommandEnvelope newNode, CommandEnvelope oldNode) {
        return newNode.command.type == Command.ADD || oldNode.command.type == Command.ADD;
    }
}
