package portal.file;

import core.*;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;

public class ListFileService extends ListFileServiceGrpc.ListFileServiceImplBase {

    private Thread thread;
    private ArrayList<FileInfo> filesInfo = null;

    public ListFileService(Thread thread) {
        this.thread = thread;
    }

    @Override
    public void sendFilesInfo(FilesInfo request, StreamObserver<RequestReply> responseObserver) {
        synchronized (this) {
            synchronized (thread) {
                thread.notify();
            }
            filesInfo = new ArrayList<>(request.getFilesList());
        }
        responseObserver.onNext(RequestReply.newBuilder().build());
        responseObserver.onCompleted();

    }

    public ArrayList<FileInfo> getFilesInfo() {
       return filesInfo;
    }
}
