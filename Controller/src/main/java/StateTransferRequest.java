import core.JGroupRequest;
import org.jgroups.Address;

public class StateTransferRequest extends JGroupRequest {
    private final ControllerState state;

    public StateTransferRequest(ControllerState state) {
        this.state = state;
    }

    public ControllerState getState() {
        return state;
    }
}
