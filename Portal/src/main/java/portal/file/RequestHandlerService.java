package portal.file;

import core.RequestInfo;
import core.RequestReply;
import core.PortalServiceGrpc;
import io.grpc.stub.StreamObserver;

public class RequestHandlerService extends PortalServiceGrpc.PortalServiceImplBase {

    private int requestId;
    private boolean isJobTaken;

    public RequestHandlerService(int requestId) {
        super();
        this.requestId = requestId;
        isJobTaken = false;
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

}
