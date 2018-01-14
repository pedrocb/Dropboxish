import com.backblaze.erasure.ReedSolomon;
import com.google.protobuf.ByteString;
import core.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public class ReceiverThread extends Thread {
    private ControllerState state;
    private JGroupRequest request;
    private Lock lock;

    public ReceiverThread(JGroupRequest request, Lock lock, ControllerState state) {
        this.request = request;
        this.lock = lock;
        this.state = state;
    }

    @Override
    public void run() {
        if(request.getType() == JGroupRequest.RequestType.UploadFile) {
            UploadFileRequest uploadFileRequest = (UploadFileRequest)request;
            lock.lock();
            System.out.println("got lock on Receiver Thread");
            String address = uploadFileRequest.getAddress();
            String fileName = uploadFileRequest.getFileName();
            long timestamp = uploadFileRequest.getTimestamp();
            try {
                ControllerWorker controllerWorker = new ControllerWorker(state, fileName, timestamp);
                controllerWorker.start();
                int port = controllerWorker.getPort();

                ManagedChannel portalChannel = ManagedChannelBuilder.forTarget(address).usePlaintext(true).build();
                PortalServiceGrpc.PortalServiceBlockingStub portalStub = PortalServiceGrpc.newBlockingStub(portalChannel);
                RequestInfo requestInfo = RequestInfo.newBuilder().setAddress("192.168.1.114").setPort(port).build();
                RequestReply requestReply = portalStub.handleRequest(requestInfo);
                System.out.println("Received reply from portal");
            }catch (Exception e){
                System.out.println("it caput");
                e.printStackTrace();
            } finally {
                lock.unlock();
                System.out.println("unlocked Receiver Thread");
            }
        } else if (request.getType() == JGroupRequest.RequestType.RegisterPool) {
            RegisterPoolRequest registerPoolRequest = (RegisterPoolRequest) request;
            System.out.println("Registering pool " + registerPoolRequest.getAddress());
            synchronized (state) {
                state.addPool(registerPoolRequest.getAddress());
            }
        } else if(request.getType() == JGroupRequest.RequestType.DownloadFile) {
            try {
                DownloadFileRequest downloadFileRequest = (DownloadFileRequest) request;
                lock.lock();
                String fileName = downloadFileRequest.getFileName();
                long timestamp = downloadFileRequest.getTimestamp();
                String address = downloadFileRequest.getAddress();
                ManagedChannel portalChannel = ManagedChannelBuilder.forTarget(address).usePlaintext(true).build();
                DownloadFileServiceGrpc.DownloadFileServiceBlockingStub portalStub = DownloadFileServiceGrpc.newBlockingStub(portalChannel);
                System.out.println(fileName);
                byte[] fileData = downloadFileWork(fileName);
                FileData_.Builder builder = FileData_.newBuilder();
                int from = 0;
                while (from < fileData.length) {
                    int size = 1024;
                    if (from + size > fileData.length) {
                        size = fileData.length - from;
                    }
                    ByteString chunk = ByteString.copyFrom(fileData, from, size);
                    builder.addData(chunk);
                    from = from + 1024;
                }
                portalStub.uploadFile(builder.build());
            } finally {
                lock.unlock();
            }
        } else {
            System.out.println("Got something else");
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
