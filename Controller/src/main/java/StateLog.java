import java.io.Serializable;
import java.util.ArrayList;

public class StateLog implements Serializable{
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

    public ArrayList<String> getArgs() {
        return args;
    }

    public void setArgs(ArrayList<String> args) {
        this.args = args;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
