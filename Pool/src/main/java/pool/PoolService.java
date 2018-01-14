package pool;

import com.google.protobuf.ByteString;
import core.*;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.util.Arrays;

public class PoolService extends PoolServiceGrpc.PoolServiceImplBase {
    public PoolService() {

    }

    @Override
    public void write(WriteBlockRequest request, StreamObserver<StatusMsg> responseObserver) {
        BlockData data = request.getData();
        String fileId = request.getBlockID().getFileId();
        int blockIndex = request.getBlockID().getBlockIndex();
        System.out.println("Writing data: " + Arrays.toString(data.getData().toByteArray()) + " on fileID = " + fileId + " block = " + blockIndex);
        String fileDir = Pool.dataDirectory + "/" + fileId;
        System.out.println("File dir "+fileDir);
        new File(fileDir).mkdirs();
        FileOutputStream fos = null;
        File dataBlockFile = new File(fileDir + "/" + blockIndex);
        try {
            dataBlockFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fos = new FileOutputStream(dataBlockFile);
            fos.write(data.getData().toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        responseObserver.onNext(StatusMsg.newBuilder().setStatusValue(0).build());
        responseObserver.onCompleted();
    }

    @Override
    public void read(ReadBlockRequest request, StreamObserver<BlockData> responseObserver) {
        String fileId = request.getBlockID().getFileId();
        int blockIndex = request.getBlockID().getBlockIndex();
        File dataBlockFile = new File(Pool.dataDirectory + "/" + fileId + "/" + blockIndex);
        byte[] b = new byte[(int) dataBlockFile.length()];
        try {
            FileInputStream fis = new FileInputStream(dataBlockFile);
            fis.read(b);
            ByteString data = ByteString.copyFrom(b);
            BlockData blockData = BlockData.newBuilder().setData(data).build();
            responseObserver.onNext(blockData);
            responseObserver.onCompleted();
        } catch (FileNotFoundException e) {
            responseObserver.onError(new Exception());
        } catch (IOException e) {
            responseObserver.onError(new Exception());
        }
    }
}
