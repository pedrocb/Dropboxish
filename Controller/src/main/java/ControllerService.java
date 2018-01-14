import com.backblaze.erasure.ReedSolomon;
import com.google.protobuf.ByteString;
import core.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ControllerService extends ControllerServiceGrpc.ControllerServiceImplBase {
    private ControllerState state;
    private String fileName;
    private long timestamp;

    public ControllerService(ControllerState state, String fileName, long timestamp) {
        this.state = state;
        this.fileName = fileName;
        this.timestamp = timestamp;
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
        if (uploadFileWork(data) == 1) {
            responseObserver.onNext(StatusMessage.newBuilder().setStatus(StatusMessage.Status.OK).build());
        } else {
            responseObserver.onNext(StatusMessage.newBuilder().setStatus(StatusMessage.Status.NOT_OK).build());
        }
        responseObserver.onCompleted();
    }

    private int uploadFileWork(byte[] file) {
        synchronized (state) {
            int from = 0;
            int blockNumber = 0;
            //For each data block that have 4 data shards, create 6 shards
            while (from < file.length) {
                int size = 1024;
                if (from + size > file.length) {
                    size = file.length - from;
                }
                byte[] data = new byte[size];
                System.arraycopy(file, from, data, 0, size);
                byte[][] shards = encondeReedSolomon(data);

                //Store each shard in a Pool
                for (int j = 0; j < shards.length; j++) {
                    boolean done = false;
                    ArrayList<String> poolsLeft = (ArrayList<String>) state.getPools().clone();
                    while (!done) {
                        if (poolsLeft.size() > 0) {
                            String chosenPool = poolsLeft.remove((int) Math.floor(Math.random() * state.getPools().size()));
                            String fileId = fileName + "|" + blockNumber;
                            try {
                                ManagedChannel poolChannel = ManagedChannelBuilder.forTarget(chosenPool).usePlaintext(true).build();
                                PoolServiceGrpc.PoolServiceBlockingStub poolStub = PoolServiceGrpc.newBlockingStub(poolChannel);
                                BlockID blockID = BlockID.newBuilder().setFileId(fileId).setBlockIndex(j).build();
                                BlockData blockData = BlockData.newBuilder().setData(ByteString.copyFrom(shards[j])).build();
                                WriteBlockRequest writeBlockRequest = WriteBlockRequest.newBuilder().setBlockID(blockID).setData(blockData).build();
                                StatusMsg statusMsg = poolStub.write(writeBlockRequest);
                                if (statusMsg.getStatusValue() == 0) {
                                    ArrayList<String> logArgs = new ArrayList<>();
                                    logArgs.add("ACTION-WRITE");
                                    logArgs.add("FILEID-" + fileId);
                                    logArgs.add("SHARDID-" + j);
                                    logArgs.add("POOL-" + chosenPool);
                                    StateLog stateLog = new StateLog(logArgs, timestamp);
                                    state.getLogs().add(stateLog);
                                    done = true;
                                }else {
                                    //write failed, try on another pool
                                }
                            } catch (Exception e) {
                                System.out.println("Comunication with pool " + chosenPool + " failed");
                            }
                        } else {
                            System.out.println("No pools online");
                            return -1;
                        }
                    }
                }

                //Simule that 2 shards are missing;
                boolean[] shardPresent = {false, true, true, true, true, false};
                int shardSize = shards[0].length;
                for (int j = 0; j < shardPresent.length; j++) {
                    if (!shardPresent[j]) {
                        shards[j] = new byte[shardSize];
                    }
                }
                byte[] decodedData = decodeReedSolomon(shards, shardPresent);
                blockNumber++;
                from += size;
            }
            System.out.println("STATE LOGS:" + state.getLogs());
            return 1;
        }

    }

    private byte[] downloadFileWork(String fileId) {
        ArrayList<StateLog> logs = state.getLogs();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Iterator logsIterator = logs.iterator();
        ArrayList<StateLog> fileIdLogs = new ArrayList<>();
        int numChunks = 0;
        while (logsIterator.hasNext()){
            StateLog log = (StateLog)logsIterator.next();
            ArrayList<String> args = log.getArgs();
            if(args.get(1).startsWith("FILEID-"+fileId)){
                fileIdLogs.add(log);
                String[] tokens = args.get(1).split("|");
                if(Integer.parseInt(tokens[tokens.length-1]) > numChunks){
                    numChunks = Integer.parseInt(tokens[tokens.length-1]);
                }
            }
        }

        //Array that for each block, has all the logs of it
        ArrayList<StateLog>[] blockIdLogs = new ArrayList[numChunks];
        for(int i = 0; i<numChunks;i++){
            blockIdLogs[i] = new ArrayList<>();
        }
        Iterator fileIdLogsIterator = fileIdLogs.iterator();
        while (fileIdLogsIterator.hasNext()){
            StateLog log = (StateLog)fileIdLogsIterator.next();
            String[] tokens = log.getArgs().get(1).split("|");
            int blockId = Integer.parseInt(tokens[tokens.length-1]);
            blockIdLogs[blockId].add(log);
        }

        byte[][] shards = new byte [6][];
        boolean[] shardPresent = new boolean[6];

        for(int blockId = 0; blockId<numChunks;blockId++){
            int shardSize = 0;
            for(int shardId = 0; shardId<6;shardId++){
                //get operation with latest timestamp
                long timestamp = 0;
                StateLog lastLog = null;
                Iterator it = blockIdLogs[blockId].iterator();
                while (it.hasNext()){
                    StateLog log = (StateLog)it.next();
                    ArrayList<String> args = log.getArgs();
                    if(args.get(2).equals("SHARDID-"+shardId) && log.getTimestamp() > timestamp){
                        timestamp = log.getTimestamp();
                        lastLog = log;
                    }
                }
                if(lastLog.getArgs().get(0).equals("ACTION-WRITE")){
                    String address = lastLog.getArgs().get(3).substring(5);
                    String poolFileId = fileId+"|"+blockId;
                    try {
                        ManagedChannel poolChannel = ManagedChannelBuilder.forTarget(address).usePlaintext(true).build();
                        PoolServiceGrpc.PoolServiceBlockingStub poolStub = PoolServiceGrpc.newBlockingStub(poolChannel);
                        BlockID b = BlockID.newBuilder().setFileId(poolFileId).setBlockIndex(shardId).build();
                        ReadBlockRequest readBlockRequest = ReadBlockRequest.newBuilder().setBlockID(b).build();
                        BlockData blockData = poolStub.read(readBlockRequest);
                        shards[shardId] = blockData.toByteArray();
                        shardPresent[shardId] = true;
                        shardSize = shards[shardId].length;
                    } catch (Exception e) {
                        //pool is down
                        shardPresent[shardId] = false;
                    }
                } else {
                    System.out.println("This BlockId was DELETED");
                }
            }

            int numberOfShardsMissing = 0;
            for(int i = 0; i<6; i++){
                if(!shardPresent[i]){
                    numberOfShardsMissing++;
                    //If we can't reconstruct the block, fail
                    if(numberOfShardsMissing > 2){
                        return null;
                    }
                    shards[i] = new byte[shardSize];
                }
            }

            byte[] blockIdData = decodeReedSolomon(shards,shardPresent);
            try {
                out.write(blockIdData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return out.toByteArray();
    }

    private byte[][] encondeReedSolomon(byte[] data) {
        int DATA_SHARDS = 4;
        int PARITY_SHARDS = 2;
        int TOTAL_SHARDS = DATA_SHARDS + PARITY_SHARDS;
        int fileSize = data.length;
        int storedSize = fileSize + Integer.BYTES;
        int shardSize = (storedSize + DATA_SHARDS - 1) / DATA_SHARDS;
        System.out.println("Original data: " + Arrays.toString(data));

        byte[] allBytes = new byte[shardSize * DATA_SHARDS];
        ByteBuffer.wrap(allBytes).putInt(fileSize).put(data);
        byte[][] shards = new byte[TOTAL_SHARDS][shardSize];
        for (int i = 0; i < DATA_SHARDS; i++) {
            System.arraycopy(allBytes, i * shardSize, shards[i], 0, shardSize);
        }

        //Create additional shards
        ReedSolomon reedSolomon = ReedSolomon.create(DATA_SHARDS, PARITY_SHARDS);
        reedSolomon.encodeParity(shards, 0, shardSize);
        return shards;
    }


    //shards that failed are empty arrays, shardsPresent[i] is true if shard[i] is present
    private byte[] decodeReedSolomon(byte[][] shards, boolean[] shardPresent) {
        int DATA_SHARDS = 4;
        int PARITY_SHARDS = 2;
        int SHARD_SIZE = shards[0].length;

        //Recover missing shards
        ReedSolomon reedSolomon = ReedSolomon.create(DATA_SHARDS, PARITY_SHARDS);
        reedSolomon.decodeMissing(shards, shardPresent, 0, SHARD_SIZE);

        byte[] allBytes = new byte[SHARD_SIZE * DATA_SHARDS];
        for (int i = 0; i < DATA_SHARDS; i++) {
            System.arraycopy(shards[i], 0, allBytes, i * SHARD_SIZE, SHARD_SIZE);
        }
        int fileSize = ByteBuffer.wrap(allBytes).getInt();
        byte[] data = new byte[fileSize];
        System.arraycopy(allBytes, Integer.BYTES, data, 0, fileSize);

        System.out.println("Recovered File: " + Arrays.toString(data));
        return data;
    }
}
