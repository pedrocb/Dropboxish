package portal.file;

import core.*;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;

public class DeleteFileService extends DeleteFileServiceGrpc.DeleteFileServiceImplBase{
    private Thread thread;
    private boolean success;
    private ArrayList<FileInfo> filesInfo = null;

    public DeleteFileService(Thread thread) {
        this.thread = thread;
    }

    @Override
    public void sendFileInfo(OperationResult request, StreamObserver<RequestReply> responseObserver) {
        synchronized (this) {
            synchronized (thread) {
                thread.notify();
            }
            this.success = request.getSuccess();
            this.filesInfo = new ArrayList<>();
        }
        responseObserver.onNext(RequestReply.newBuilder().build());
        responseObserver.onCompleted();

    }

    public ArrayList<FileInfo> getFilesInfo() {
        return filesInfo;
    }

    public boolean getSuccess(){
        return success;
    }
}
