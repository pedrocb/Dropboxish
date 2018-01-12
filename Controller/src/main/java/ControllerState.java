import java.io.Serializable;
import java.util.ArrayList;

public class ControllerState implements Serializable {
    private ArrayList<String> storagePools;
    private ArrayList<String> files;

    public ControllerState() {
        storagePools = new ArrayList<String>();
        files = new ArrayList<String>();
    }

    public void addPool(String address) {
        storagePools.add(address);
    }

    @Override
    public String toString() {
        return "Pools: " + storagePools;
    }
}
