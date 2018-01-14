import java.io.Serializable;
import java.util.ArrayList;

public class StateLog implements Serializable {
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

    @Override
    public String toString(){
       return args+"TIMESTAMP: "+timestamp+"\n";
    }
}
