package portal.file;

import com.google.protobuf.ByteString;
import core.FileData;
import core.RequestInfo;
import core.RequestReply;
import core.PortalServiceGrpc;
import io.grpc.stub.StreamObserver;

import static java.util.Arrays.copyOfRange;

public class RequestHandlerService extends PortalServiceGrpc.PortalServiceImplBase {

    private int requestId;
    private boolean isJobTaken;
    private byte[] fileData;

    public RequestHandlerService(int requestId, byte[] fileData) {
        super();
        this.requestId = requestId;
        isJobTaken = false;
        this.fileData = fileData;
    }

    @Override
    public void handleRequest(RequestInfo request, StreamObserver<RequestReply> responseObserver) {
        RequestReply reply;
        synchronized (this) {
            if (request.getId() == requestId && !isJobTaken) {
                reply = RequestReply.newBuilder().setGotTheJob(true).build();
                isJobTaken = true;
                System.out.println("Replied with true to request " + requestId);
            } else {
                reply = RequestReply.newBuilder().setGotTheJob(false).build();
                System.out.println("Replied with false to request " + requestId);
            }
        }
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void uploadFileRequest(RequestInfo request, StreamObserver<FileData> responseObserver) {

        int chunkSize = 1024, from = 0;
        int fileSize = fileData.length;
        while (from < fileSize) {
            int to = from + chunkSize >= fileSize ? fileSize : from + chunkSize;
            byte[] chunk = copyOfRange(fileData, from, to);
            FileData fileData = FileData.newBuilder().setData(ByteString.copyFrom(chunk)).build();
            responseObserver.onNext(fileData);
            from += chunkSize;
        }

        responseObserver.onCompleted();
    }
}
