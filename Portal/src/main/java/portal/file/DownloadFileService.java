package portal.file;

import core.ControllerServiceGrpc;
import core.PortalServiceGrpc;
import core.RequestInfo;
import core.RequestReply;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.locks.ReentrantLock;

public class DownloadFileService extends PortalServiceGrpc.PortalServiceImplBase {
    private ReentrantLock lock;

    public DownloadFileService(ReentrantLock lock) {
        this.lock = lock;
    }

    @Override
    public void handleRequest(RequestInfo request, StreamObserver<RequestReply> responseObserver) {
        lock.unlock();
        String address = request.getAddress();
        int port = request.getPort();
        ManagedChannel channel = ManagedChannelBuilder.forAddress(address, port).usePlaintext(true).build();
        ControllerServiceGrpc.ControllerServiceBlockingStub stub = ControllerServiceGrpc.newBlockingStub(channel);
    }
}
