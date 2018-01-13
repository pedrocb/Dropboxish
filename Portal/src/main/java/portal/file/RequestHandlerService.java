package portal.file;

import com.google.protobuf.ByteString;
import core.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.locks.ReentrantLock;

public class RequestHandlerService extends PortalServiceGrpc.PortalServiceImplBase {

    private byte[] fileData;
    private boolean sentFile;
    private javax.ws.rs.core.Response response;
    private ReentrantLock lock;

    public RequestHandlerService(byte[] fileData, ReentrantLock lock) {
        super();
        this.fileData = fileData;
        this.response = javax.ws.rs.core.Response.status(500).build();
        this.lock = lock;
        lock.lock();
    }

    @Override
    public void handleRequest(RequestInfo request, StreamObserver<RequestReply> responseObserver) {
        lock.unlock();
        String address = request.getAddress();
        int port = request.getPort();
        ManagedChannel channel = ManagedChannelBuilder.forAddress(address,port).usePlaintext(true).build();
        ControllerServiceGrpc.ControllerServiceBlockingStub controllerStub = ControllerServiceGrpc.newBlockingStub(channel);
        responseObserver.onCompleted();
    }
}
