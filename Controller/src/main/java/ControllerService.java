import core.*;
import io.grpc.stub.StreamObserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ControllerService extends ControllerServiceGrpc.ControllerServiceImplBase {

    public ControllerService() {

    }

    @Override
    public void registerPool(PoolInfo request, StreamObserver<Empty> responseObserver) {
        super.registerPool(request, responseObserver);
    }

    @Override
    public void uploadFile(FileData request, StreamObserver<StatusMessage> responseObserver) {
        int nChunks = request.getDataCount();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < nChunks; i++) {
            try {
                out.write(request.getData(i).toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        byte[] data = out.toByteArray();
        System.out.println("File uploaded");
        responseObserver.onNext(StatusMessage.newBuilder().setStatus(StatusMessage.Status.OK).build());
        responseObserver.onCompleted();
    }

    /*
    @Override
    public StreamObserver<FileData> uploadFile (final StreamObserver<StatusMessage> responseObserver){
        return new StreamObserver<FileData>() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            @Override
            public void onNext(FileData fileData) {
                try {
                    out.write(fileData.getData().toByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Controller service onError called");
            }

            @Override
            public void onCompleted() {
                System.out.println("Waiting 5 seconds");
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                responseObserver.onNext(StatusMessage.newBuilder().setStatusValue(0).build());
                responseObserver.onCompleted();
                byte[] data = out.toByteArray();
                System.out.println("File upload completed: "+ Arrays.toString(data));
            }
        };
    }
    */

    private void uploadFileWork(String requestId, PortalServiceGrpc.PortalServiceBlockingStub stub){
        /*RequestInfo request = RequestInfo.newBuilder().build();
        Iterator<FileData> fileDataIterator;

        try {
            fileDataIterator = stub.uploadFileRequest(request);
            for(int i = 1; fileDataIterator.hasNext(); i++){
                FileData fileData = fileDataIterator.next();
                System.out.println("Recieved piece #"+i+" lenght "+fileData.getData().toByteArray().length);
                byte [] data = fileData.getData().toByteArray();
                byte[][] shards = encondeReedSolomon(data);

                for(int j=0; j<shards.length; j++){
                    try{
                        ManagedChannel poolChannel = ManagedChannelBuilder.forTarget(state.getPools().get(0)).usePlaintext(true).build();
                        PoolServiceGrpc.PoolServiceBlockingStub poolStub = PoolServiceGrpc.newBlockingStub(poolChannel);
                        BlockID blockID = BlockID.newBuilder().setFileId(UUID.randomUUID().toString()).setBlockIndex(j).build();
                        BlockData blockData = BlockData.newBuilder().setData(ByteString.copyFrom(shards[j])).build();
                        WriteBlockRequest writeBlockRequest = WriteBlockRequest.newBuilder().setBlockID(blockID).setData(blockData).build();
                        StatusMsg statusMsg = poolStub.write(writeBlockRequest);
                        System.out.println(statusMsg.getStatusValue());
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                //Simule that 2 shards are missing;
                boolean [] shardPresent = {false, true, true, true, true, false};
                int shardSize = shards[0].length;
                for(int j = 0; j < shardPresent.length; j++){
                    if(!shardPresent[j]){
                        shards[j] = new byte[shardSize];
                    }
                }
                byte [] decodedData = decodeReedSolomon(shards,shardPresent);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private byte[][] encondeReedSolomon (byte[] data) {
        int DATA_SHARDS = 4;
        int PARITY_SHARDS = 2;
        int TOTAL_SHARDS = DATA_SHARDS + PARITY_SHARDS;
        int fileSize = data.length;
        int storedSize = fileSize + Integer.BYTES;
        int shardSize = (storedSize + DATA_SHARDS - 1)/DATA_SHARDS;
        System.out.println("Original data: "+Arrays.toString(data));

        byte [] allBytes = new byte[shardSize * DATA_SHARDS];
        ByteBuffer.wrap(allBytes).putInt(fileSize).put(data);
        byte [][] shards = new byte[TOTAL_SHARDS][shardSize];
        for(int i = 0; i < DATA_SHARDS; i++){
            System.arraycopy(allBytes, i*shardSize, shards[i],0,shardSize);
        }

        //Create additional shards
        ReedSolomon reedSolomon = ReedSolomon.create(DATA_SHARDS, PARITY_SHARDS);
        reedSolomon.encodeParity(shards,0,shardSize);
        return shards;
    }


    //shards that failed are empty arrays, shardsPresent[i] is true if shard[i] is present
    private byte[] decodeReedSolomon (byte [][] shards, boolean [] shardPresent) {
        int DATA_SHARDS = 4;
        int PARITY_SHARDS = 2;
        int SHARD_SIZE = shards[0].length;

        //Recover missing shards
        ReedSolomon reedSolomon = ReedSolomon.create(DATA_SHARDS, PARITY_SHARDS);
        reedSolomon.decodeMissing(shards,shardPresent, 0, SHARD_SIZE);

        byte[] allBytes = new byte[SHARD_SIZE*DATA_SHARDS];
        for(int i = 0; i < DATA_SHARDS; i++){
            System.arraycopy(shards[i],0,allBytes,i*SHARD_SIZE,SHARD_SIZE);
        }
        int fileSize = ByteBuffer.wrap(allBytes).getInt();
        byte [] data = new byte[fileSize];
        System.arraycopy(allBytes,Integer.BYTES,data, 0, fileSize);

        System.out.println("Recovered File: "+Arrays.toString(data));
        return data;
        */
    }
}
