package demo.list.hibrid;

public enum ListCommand {
    SET(1),
    GET(2),
    TRANSFER(3);

    public final int value;

    ListCommand(int value) {
        this.value = value;
    }
}
