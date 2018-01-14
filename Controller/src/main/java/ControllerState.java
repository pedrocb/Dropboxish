import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ControllerState implements Serializable {
    private ArrayList<String> storagePools;
    private ArrayList<String> files;
    private ArrayList<StateLog> logs;

    public ControllerState() {
        storagePools = new ArrayList<String>();
        files = new ArrayList<String>();
        logs = new ArrayList<StateLog>();
    }

    public void addPool(String address) {
        storagePools.add(address);
    }

    @Override
    public String toString() {
        return "Pools: " + storagePools+"\n"+"Logs: "+logs;
    }

    public ArrayList<String> getPools() {
        return storagePools;
    }

    public ArrayList<StateLog> getLogs() {
        return logs;
    }
}
