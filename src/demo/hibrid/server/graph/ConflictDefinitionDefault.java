package demo.hibrid.server.graph;

import demo.hibrid.request.Command;
import demo.hibrid.server.ServerCommand;

public class ConflictDefinitionDefault implements ConflictDefinition<ServerCommand> {

    @Override
    public boolean isDependent(ServerCommand r1, ServerCommand r2) {
        return r1.getCommand().getType() == Command.ADD || r2.getCommand().getType() == Command.ADD;
    }
}
