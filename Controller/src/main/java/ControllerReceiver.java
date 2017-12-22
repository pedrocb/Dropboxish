import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

/*
    Receiver is reponsible to implement fuctions that modulate a controller's behaviour
 */

public class ControllerReceiver extends ReceiverAdapter{

    public ControllerReceiver() {

    }

    //when a new message is received
    public void receive(Message msg) {
        String line="[ Controller " + msg.getSrc() + "] said: " + msg.getObject();
        System.out.println(line);
    }

    //when a new controller is added or removed!!
    public void viewAccepted(View view) {
        super.viewAccepted(view);
        System.out.println("New View :: "+view);
    }
}
