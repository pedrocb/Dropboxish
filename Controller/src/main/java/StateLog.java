import java.util.ArrayList;

public class StateLog {
    public enum Type {
        Write,
    }
    private ArrayList<String> args;
    private long timestamp;
    private boolean executed;

    public StateLog(ArrayList<String> args, long timestamp) {
        this.timestamp = timestamp;
        this.args = args;
        executed = false;
    }
}
