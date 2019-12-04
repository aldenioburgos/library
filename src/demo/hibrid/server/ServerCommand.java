package demo.hibrid.server;

import demo.hibrid.request.Command;

public class ServerCommand  {

    private int requestId;
    private Command command;

    public ServerCommand(int requestId, Command command) {
        this.requestId = requestId;
        this.command = command;
    }

    public int getRequestId() {
        return requestId;
    }

    public Command getCommand() {
        return command;
    }
}
