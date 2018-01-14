package portal.file;

import core.ControllerServiceGrpc;
import core.PortalServiceGrpc;
import core.RequestInfo;
import core.RequestReply;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import javax.ws.rs.core.Response;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadFileService extends PortalServiceGrpc.PortalServiceImplBase {

    private Response response = null;
    private Thread thread = new Thread();

    public DownloadFileService(Thread thread) {
        this.thread = thread;
    }

    @Override
    public void handleRequest(RequestInfo request, StreamObserver<RequestReply> responseObserver) {
        String address = request.getAddress();
        int port = request.getPort();
        ManagedChannel channel = ManagedChannelBuilder.forAddress(address, port).usePlaintext(true).build();
        ControllerServiceGrpc.ControllerServiceBlockingStub stub = ControllerServiceGrpc.newBlockingStub(channel);
    }

    public Response getResponse() {
        return response;
    }
}
