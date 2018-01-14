import com.google.protobuf.ByteString;
import core.*;
import com.backblaze.erasure.ReedSolomon;
import core.PortalServiceGrpc;
import core.RequestInfo;
import core.RequestReply;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import java.io.*;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;


/*
    Receiver is reponsible to implement fuctions that modulate a controller's behaviour
 */

public class ControllerReceiver extends ReceiverAdapter {
    ControllerState state = new ControllerState();
    Lock lock;

    public ControllerReceiver(ControllerState state) {
        this.state = state;
        this.lock = lock;
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
        if(request.getType() == JGroupRequest.RequestType.UploadFile) {
            UploadFileRequest uploadFileRequest = (UploadFileRequest)request;
            System.out.println("Got a upload file request from " + msg.getSrc());
            lock.lock();
            String address = uploadFileRequest.getAddress();
            String fileName = uploadFileRequest.getFileName();
            long timestamp = uploadFileRequest.getTimestamp();
            try {
                ControllerWorker controllerWorker = new ControllerWorker(state, fileName, timestamp);
                controllerWorker.start();
                int port = controllerWorker.getPort();

                ManagedChannel portalChannel = ManagedChannelBuilder.forTarget(address).usePlaintext(true).build();
                PortalServiceGrpc.PortalServiceBlockingStub portalStub = PortalServiceGrpc.newBlockingStub(portalChannel);
                RequestInfo requestInfo = RequestInfo.newBuilder().setAddress("localhost").setPort(port).build();
                RequestReply requestReply = portalStub.handleRequest(requestInfo);
            }catch (Exception e){
                System.out.println("it caput");
                e.printStackTrace();
            } finally {
                lock.unlock();
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


    //when a new controller is added or removed!!
    public void viewAccepted(View view) {
        super.viewAccepted(view);
        System.out.println("New View :: " + view);
    }

    public void setLock(Lock lock) {
        this.lock = lock;
    }

}
