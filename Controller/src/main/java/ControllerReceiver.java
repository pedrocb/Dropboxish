import core.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;


/*
    Receiver is reponsible to implement fuctions that modulate a controller's behaviour
 */

public class ControllerReceiver extends ReceiverAdapter {
    ControllerState state = new ControllerState();

    public ControllerReceiver(ControllerState state) {
        this.state = state;
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized (state) {
            Util.objectToStream(state, new DataOutputStream(output));
        }
        System.out.println("getState Called");
        super.getState(output);
    }

    @Override
    public void setState(InputStream input) throws Exception {
        System.out.println("setState Called");
        ControllerState state = Util.objectFromStream(new DataInputStream(input));
        synchronized (this.state) {
            this.state.getPools().clear();
            this.state.getPools().addAll(state.getPools());
        }
        System.out.println(state);
    }

    public ControllerReceiver() {

    }

    //when a new request is received
    public void receive(Message msg) {
        JGroupRequest request = msg.getObject();
        System.out.println("got msg");
        if(request.getType() == JGroupRequest.RequestType.UploadFile) {
            System.out.println("Got a upload file request from " + msg.getSrc());
            String[] args = ((String) msg.getObject()).split(" ");
            int requestId = Integer.parseInt(args[0]);
            String address = args[1];
            int port = Integer.parseInt(args[2]);
            String type = args[3];
            // Try to connect to portal, only the first will get the job
            System.out.println("Requesting job " + requestId + " to " + address + ":" + port);
            try {
                ManagedChannel portalChannel = ManagedChannelBuilder.forTarget(address + ":" + port).usePlaintext(true).build();
                PortalServiceGrpc.PortalServiceBlockingStub portalStub = PortalServiceGrpc.newBlockingStub(portalChannel);
                RequestInfo requestInfo = RequestInfo.newBuilder().setId(requestId).build();
                RequestReply requestReply = portalStub.handleRequest(requestInfo);
                Boolean gotTheJob = requestReply.getGotTheJob();
                System.out.println("Got the job " + requestId + ": " + gotTheJob);
                if (gotTheJob) {
                    if (type.equals("NewFile")) {
                        uploadFileWork(requestId, portalStub);
                    } else {
                        fakeSomeWork(requestId);
                    }
                }
            } catch (Exception e) {
                System.out.println("it caput" + e.getMessage());
            }
        } else if (request.getType() == JGroupRequest.RequestType.RegisterPool){
            RegisterPoolRequest registerPoolRequest = (RegisterPoolRequest) request;
            System.out.println("Registering pool " + registerPoolRequest.getAddress());
            synchronized (state) {
                state.addPool(registerPoolRequest.getAddress());
            }
        } else {
            System.out.println("Got something else");
        }
    }

    private void uploadFileWork(int requestId, PortalServiceGrpc.PortalServiceBlockingStub stub){
        RequestInfo request = RequestInfo.newBuilder().setId(requestId).build();
        Iterator<FileData> fileDataIterator;

        try {
            fileDataIterator = stub.uploadFileRequest(request);
            for(int i = 1; fileDataIterator.hasNext(); i++){
                FileData fileData = fileDataIterator.next();
                System.out.println("Recieved piece #"+i+" lenght "+fileData.getData().toByteArray().length);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void fakeSomeWork(int requestId) {
        for (int i = 0; i < 10; i++) {
            System.out.println("Im working on " + requestId);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //when a new controller is added or removed!!
    public void viewAccepted(View view) {
        super.viewAccepted(view);
        System.out.println("New View :: " + view);
    }
}
