package demo.hibrid.server.graph;

import demo.hibrid.request.Command;
import demo.hibrid.server.ServerCommand;

public class ConflictDefinitionDefault implements ConflictDefinition<ServerCommand> {

    @Override
    public boolean isDependent(ServerCommand newNode, ServerCommand oldNode) {
        return newNode.command.type == Command.ADD || oldNode.command.type == Command.ADD;
    }
}
