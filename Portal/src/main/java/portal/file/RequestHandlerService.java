package portal.file;

import com.google.protobuf.ByteString;
import core.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class RequestHandlerService extends PortalServiceGrpc.PortalServiceImplBase {

    private byte[] fileData;
    private boolean sentFile;
    private javax.ws.rs.core.Response response;
    private Thread thread;

    public RequestHandlerService(byte[] fileData, Thread thread) {
        this.fileData = fileData;
        this.response = javax.ws.rs.core.Response.status(500).build();
        this.thread = thread;
        this.response = null;
    }

    @Override
    public void handleRequest(RequestInfo request, StreamObserver<RequestReply> responseObserver) {
        synchronized (this) {
            synchronized (thread) {
                System.out.println("Notifying endpoint thread...");
                thread.notify();
            }
            String address = request.getAddress();
            int port = request.getPort();
            System.out.println("Going to send file to " + address + ":" + port);
            ManagedChannel channel = ManagedChannelBuilder.forAddress(address, port).usePlaintext(true).build();
            ControllerServiceGrpc.ControllerServiceBlockingStub controllerStub = ControllerServiceGrpc.newBlockingStub(channel);
            try {
                System.out.println("Sleeping 5 seconds");
                Thread.sleep(5000);
                System.out.println("Awaken");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        responseObserver.onCompleted();
    }

    public Response getResponse() {
        return response;
    }
}
