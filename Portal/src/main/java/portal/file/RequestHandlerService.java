package portal.file;

import com.google.protobuf.ByteString;
import core.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.Arrays;
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
        try{
            //lock.unlock();
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("Received message from controller");
        String address = request.getAddress();
        int port = request.getPort();
        try {
            System.out.println("Sending message to "+address+":"+port);
            ManagedChannel channel = ManagedChannelBuilder.forTarget(address+":"+port).usePlaintext(true).build();
            //ControllerServiceGrpc.ControllerServiceBlockingStub controllerStub = ControllerServiceGrpc.newBlockingStub(channel);
            ControllerServiceGrpc.ControllerServiceStub controllerStub = ControllerServiceGrpc.newStub(channel);
            StreamObserver<StatusMessage> statusResponseObserver = new StreamObserver<StatusMessage>() {
                @Override
                public void onNext(StatusMessage status) {
                    System.out.println("portal on next");
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("portal on error");
                    responseObserver.onError(new Throwable());
                }

                @Override
                public void onCompleted() {
                    System.out.println("portal on complete");
                    responseObserver.onNext(RequestReply.newBuilder().setSucess(true).build());
                }
            };
            StreamObserver<FileData> statusRequestObserver = controllerStub.uploadFile(statusResponseObserver);
            int from = 0;
            while (from < fileData.length) {
                try {
                    int size = 1024;
                    if(from + size > fileData.length){
                        size = fileData.length-from;
                    }
                    ByteString chunk = ByteString.copyFrom(fileData, from, size);
                    FileData fileData = FileData.newBuilder().setData(chunk).build();
                    statusRequestObserver.onNext(fileData);
                    System.out.println("Sent chunk "+ Arrays.toString(chunk.toByteArray()));
                    from = from + 1024;
                } catch (Exception e) {
                    System.out.println("Calling on error statusRequestObserver");
                    e.printStackTrace();
                    statusRequestObserver.onError(e);
                }
            }
            statusRequestObserver.onCompleted();
            System.out.println("Message Over");
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }


        responseObserver.onCompleted();
    }
}
