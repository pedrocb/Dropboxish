package portal.file;

import core.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DownloadFileService extends DownloadFileServiceGrpc.DownloadFileServiceImplBase {

    private byte[] fileData = null;
    private Thread thread = new Thread();

    public DownloadFileService(Thread thread) {
        this.thread = thread;
    }

    @Override
    public void uploadFile(FileData_ request, StreamObserver<RequestReply> responseObserver) {
        synchronized (this) {
            synchronized (thread) {
                thread.notify();
            }
            int nChunks = request.getDataCount();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int i = 0; i < nChunks; i++) {
                try {
                    out.write(request.getData(i).toByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            fileData = out.toByteArray();
        }
    }


    public byte[] getFile() {
        return fileData;
    }
}
