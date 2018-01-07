import core.PortalServiceGrpc;
import core.RequestInfo;
import core.RequestReply;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;


/*
    Receiver is reponsible to implement fuctions that modulate a controller's behaviour
 */

public class ControllerReceiver extends ReceiverAdapter {

    public ControllerReceiver() {

    }

    //when a new request is received
    public void receive(Message msg) {
        String line = "[ Portal " + msg.getSrc() + "] said: " + msg.getObject();
        System.out.println(line);
        String[] args = ((String) msg.getObject()).split(" ");
        int requestId = Integer.parseInt(args[0]);
        String address = args[1];
        int port = Integer.parseInt(args[2]);
        String type = args[3];
        // Try to connect to portal, only the first will get the job
        System.out.println("Requesting job " +requestId+" to " + address + ":" + port);
        try {
            ManagedChannel portalChannel = ManagedChannelBuilder.forTarget(address + ":" + port).usePlaintext(true).build();
            PortalServiceGrpc.PortalServiceBlockingStub portalStub = PortalServiceGrpc.newBlockingStub(portalChannel);
            RequestInfo requestInfo = RequestInfo.newBuilder().setId(requestId).build();
            RequestReply requestReply = portalStub.handleRequest(requestInfo);
            Boolean gotTheJob = requestReply.getGotTheJob();
            System.out.println("Got the job: " + gotTheJob);
        } catch (Exception e) {
            System.out.println("it caput" + e.getMessage());
        }

    }

    //when a new controller is added or removed!!
    public void viewAccepted(View view) {
        super.viewAccepted(view);
        System.out.println("New View :: " + view);
    }
}
