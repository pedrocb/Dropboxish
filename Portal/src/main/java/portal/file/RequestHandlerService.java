package portal.file;

import com.google.protobuf.ByteString;
import core.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public class RequestHandlerService extends PortalServiceGrpc.PortalServiceImplBase {

    private byte[] fileData;
    private boolean sentFile;
    private javax.ws.rs.core.Response response;
    private Thread thread;

    public RequestHandlerService(byte[] fileData, Thread thread) {
        this.fileData = fileData;
        System.out.println(fileData.length);
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
            System.out.println("Received message from controller");
            String address = request.getAddress();
            int port = request.getPort();
            try {
                System.out.println("Sending message to " + address + ":" + port);
                ManagedChannel channel = ManagedChannelBuilder.forTarget(address + ":" + port).usePlaintext(true).build();
                ControllerServiceGrpc.ControllerServiceBlockingStub controllerStub = ControllerServiceGrpc.newBlockingStub(channel);

                FileData.Builder builder = FileData.newBuilder();
                int from = 0;
                while (from < fileData.length) {
                    int size = 1024;
                    if (from + size > fileData.length) {
                        size = fileData.length - from;
                    }
                    ByteString chunk = ByteString.copyFrom(fileData, from, size);
                    builder.addData(chunk);
                    System.out.println("Sent chunk " + Arrays.toString(chunk.toByteArray()));
                    from = from + 1024;
                }
                controllerStub.uploadFile(builder.build());
                response = Response.status(200).build();

                /*ControllerServiceGrpc.ControllerServiceStub controllerStub = ControllerServiceGrpc.newStub(channel);
                StreamObserver<StatusMessage> statusResponseObserver = new StreamObserver<StatusMessage>() {
                    @Override
                    public void onNext(StatusMessage status) {
                        response = Response.status(200).build();
                        System.out.println("portal on next");
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("portal on error");
                        responseObserver.onError(new Throwable());
                        responseObserver.onCompleted();
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("portal on complete");
                        responseObserver.onNext(RequestReply.newBuilder().setSucess(true).build());
                        responseObserver.onCompleted();
                    }
                };
                StreamObserver<FileData> statusRequestObserver = controllerStub.uploadFile(statusResponseObserver);
                int from = 0;
                while (from < fileData.length) {
                    try {
                        int size = 1024;
                        if (from + size > fileData.length) {
                            size = fileData.length - from;
                        }
                        ByteString chunk = ByteString.copyFrom(fileData, from, size);
                        FileData fileData = FileData.newBuilder().setData(chunk).build();
                        statusRequestObserver.onNext(fileData);
                        System.out.println("Sent chunk " + Arrays.toString(chunk.toByteArray()));
                        from = from + 1024;
                    } catch (Exception e) {
                        System.out.println("Calling on error statusRequestObserver");
                        e.printStackTrace();
                        statusRequestObserver.onError(e);
                    }
                } */
                System.out.println("Message Over");
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("left handle request");

    }

    public Response getResponse() {
        return response;
    }
}
