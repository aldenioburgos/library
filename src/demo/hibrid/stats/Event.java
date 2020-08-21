package demo.hibrid.stats;

public class Event {

    private final Integer workerId;
    private final EventType type;
    private final Integer requestId;
    private final Integer commandId;
    private final Integer lateSchedulerId;
    private final long timestamp = System.nanoTime();

    public Event(EventType type, Integer requestId, Integer commandId, Integer lateSchedulerId, Integer workerId) {
        this.type = type;
        this.requestId = requestId;
        this.commandId = commandId;
        this.lateSchedulerId = lateSchedulerId;
        this.workerId = workerId;
    }


    public EventType getType() {
        return type;
    }

    public Integer getRequestId() {
        return requestId;
    }

    @Override
    public String toString() {
        return "Event{" +
                "\ttype=" + type +
                "\trequestId=" + requestId +
                "\tcommandId=" + commandId +
                "\tlateSchedulerId=" + lateSchedulerId +
                "\tworkerId=" + workerId +
                "\ttimestamp=" + timestamp +
                '}';
    }
}
