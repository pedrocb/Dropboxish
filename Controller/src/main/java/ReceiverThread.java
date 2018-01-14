import core.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.rmi.registry.LocateRegistry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public class ReceiverThread extends Thread {
    private ControllerState state;
    private JGroupRequest request;
    private Lock lock;

    public ReceiverThread(JGroupRequest request, Lock lock, ControllerState state) {
        this.request = request;
        this.lock = lock;
        this.state = state;
    }

    @Override
    public void run() {
        if(request.getType() == JGroupRequest.RequestType.UploadFile) {
            UploadFileRequest uploadFileRequest = (UploadFileRequest)request;
            lock.lock();
            System.out.println("got lock on Receiver Thread");
            String address = uploadFileRequest.getAddress();
            String fileName = uploadFileRequest.getFileName();
            long timestamp = uploadFileRequest.getTimestamp();
            try {
                ControllerWorker controllerWorker = new ControllerWorker(state, fileName, timestamp);
                controllerWorker.start();
                int port = controllerWorker.getPort();

                ManagedChannel portalChannel = ManagedChannelBuilder.forTarget(address).usePlaintext(true).build();
                PortalServiceGrpc.PortalServiceBlockingStub portalStub = PortalServiceGrpc.newBlockingStub(portalChannel);
                RequestInfo requestInfo = RequestInfo.newBuilder().setAddress("192.168.1.114").setPort(port).build();
                RequestReply requestReply = portalStub.handleRequest(requestInfo);
                System.out.println("Received reply from portal");
            }catch (Exception e){
                System.out.println("it caput");
                e.printStackTrace();
            } finally {
                lock.unlock();
                System.out.println("unlocked Receiver Thread");
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
}
