import com.backblaze.erasure.ReedSolomon;
import com.google.protobuf.ByteString;
import core.*;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.jgroups.JChannel;
import org.jgroups.Message;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public class ReceiverThread extends Thread {
    private ControllerState state;
    private JGroupRequest request;
    private JChannel channel;
    private Lock lock;
    private String selfAddress;
    public static Properties config;

    public ReceiverThread(JGroupRequest request, Lock lock, ControllerState state, JChannel channel) {
        this.request = request;
        this.lock = lock;
        this.state = state;
        this.channel = channel;

        try{
            loadConfig();
            selfAddress = config.getProperty("selfAddress","http://localhost");
        } catch (Exception e){
            System.out.println("Controller config missing, defaulting to http://localhost");
            selfAddress = "http://localhost";
        }
        System.out.println("Self address = "+ selfAddress);
    }

    @Override
    public void run() {
        if(request.getType() == JGroupRequest.RequestType.UploadFile) {
            UploadFileRequest uploadFileRequest = (UploadFileRequest)request;
            lock.lock();
            System.out.println("got lock on Receiver Thread");
            synchronized (state) {
                for(StateLog log : state.getLogs()) {
                    if(request.getTimestamp() == log.getTimestamp()) {
                        lock.unlock();
                        return;
                    }
                }
            }
            String address = uploadFileRequest.getAddress();
            String fileName = uploadFileRequest.getFileName();
            long timestamp = uploadFileRequest.getTimestamp();
            try {
                ControllerWorker controllerWorker = new ControllerWorker(state, fileName, timestamp);
                controllerWorker.start();
                int port = controllerWorker.getPort();
                ManagedChannel portalChannel = ManagedChannelBuilder.forTarget(address).usePlaintext(true).build();
                PortalServiceGrpc.PortalServiceBlockingStub portalStub = PortalServiceGrpc.newBlockingStub(portalChannel);
                RequestInfo requestInfo = RequestInfo.newBuilder().setAddress(selfAddress).setPort(port).build();
                RequestReply requestReply = portalStub.handleRequest(requestInfo);
                System.out.println("Received reply from portal");
                synchronized (state) {
                    Message msg = new Message(null, new StateTransferRequest(state));
                    msg.setFlag(Message.Flag.RSVP);
                    channel.send(null, new StateTransferRequest(state));
                }
                TimeUnit.SECONDS.sleep(2);
            }catch (Exception e){
                System.out.println("it caput");
                e.printStackTrace();
            } finally {
                lock.unlock();
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
                System.out.println("Got download");
                String fileName = downloadFileRequest.getFileName();
                long timestamp = downloadFileRequest.getTimestamp();
                String address = downloadFileRequest.getAddress();
                ManagedChannel portalChannel = ManagedChannelBuilder.forTarget(address).usePlaintext(true).build();
                DownloadFileServiceGrpc.DownloadFileServiceBlockingStub portalStub = DownloadFileServiceGrpc.newBlockingStub(portalChannel);
                System.out.println(fileName);
                byte[] fileData = downloadFileWork(fileName);
                if(fileData == null){
                    portalStub.uploadFile(FileData_.newBuilder().build());
                }
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (request.getType() == JGroupRequest.RequestType.ListFiles) {
            ListFilesRequest listFilesRequest = (ListFilesRequest) request;
            long timestamp = listFilesRequest.getTimestamp();
            String address = listFilesRequest.getAddress();
            ManagedChannel portalChannel = ManagedChannelBuilder.forTarget(address).usePlaintext(true).build();
            ListFileServiceGrpc.ListFileServiceBlockingStub portalStub = ListFileServiceGrpc.newBlockingStub(portalChannel);
            portalStub.sendFilesInfo(FilesInfo.newBuilder().addAllFiles(getListFiles()).build());
        } else if (request.getType() == JGroupRequest.RequestType.DeleteFile) {
            System.out.println("deleting file");
            lock.lock();
            try {
                synchronized (state) {
                    for (StateLog log : state.getLogs()) {
                        if (request.getTimestamp() == log.getTimestamp()) {
                            lock.unlock();
                            return;
                        }
                    }
                }
                DeleteFileRequest deleteFileRequest = (DeleteFileRequest) request;
                String address = deleteFileRequest.getAddress();
                ManagedChannel portalChannel = ManagedChannelBuilder.forTarget(address).usePlaintext(true).build();
                DeleteFileServiceGrpc.DeleteFileServiceBlockingStub portalStub = DeleteFileServiceGrpc.newBlockingStub(portalChannel);
                deleteFileWork(deleteFileRequest.getFileName());
                portalStub.sendFileInfo(OperationResult.newBuilder().setSuccess(true).build());

                synchronized (state) {
                    Message msg = new Message(null, new StateTransferRequest(state));
                    msg.setFlag(Message.Flag.RSVP);
                    channel.send(null, new StateTransferRequest(state));
                }
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        } else {
            StateTransferRequest stateTransferRequest = (StateTransferRequest) request;
            try {
                synchronized (state) {
                    state.getLogs().clear();
                    state.getPools().clear();
                    state.getPools().addAll(stateTransferRequest.getState().getPools());
                    state.getLogs().addAll(stateTransferRequest.getState().getLogs());
                }
                System.out.println("Updated state");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayList<FileInfo> getListFiles() {
        ArrayList<StateLog> logs = state.getLogs();
        logs.sort(Comparator.comparingLong(StateLog::getTimestamp));
        System.out.println(logs);
        Iterator logsIterator = logs.iterator();
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        ArrayList<FileInfo> filesInfos = new ArrayList<>();
        while (logsIterator.hasNext()){
            StateLog log = (StateLog)logsIterator.next();
            ArrayList<String> args = log.getArgs();
            if(args.get(0).equals("ACTION-WRITE")){
                String fileName = args.get(1).split("#")[0].substring(7);
                System.out.println(fileName);
                hashMap.put(fileName, hashMap.getOrDefault(fileName, 0) + 1);
            }
        }
        for(String key : hashMap.keySet()) {
            filesInfos.add(FileInfo.newBuilder().setFileName(key).setFileSize((int) (hashMap.get(key) * 1024.0 * 4.0 / 6.0)).build());
        }
        return filesInfos;
    }

    private void deleteFileWork(String fileId){
        ArrayList<StateLog> logs = state.getLogs();
        Iterator it = logs.iterator();
        ArrayList<StateLog> newLogs = new ArrayList<>();
        long timestamp = request.getTimestamp();
        while (it.hasNext()){
           StateLog log = (StateLog)it.next();
           ArrayList<String>args = log.getArgs();
           if(args.get(1).startsWith("FILEID-"+fileId)){
               ArrayList<String>newArgs = (ArrayList<String>)args.clone();
               newArgs.set(0,"ACTION-DELETE");
               StateLog newLog = new StateLog(newArgs,timestamp);
               newLogs.add(newLog);
           }
        }
        it = newLogs.iterator();
        while (it.hasNext()){
            logs.add((StateLog)it.next());
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
                String[] tokens = args.get(1).split("#");
                if(Integer.parseInt(tokens[tokens.length-1]) > numChunks){
                    numChunks = Integer.parseInt(tokens[tokens.length-1]);
                }
            }
        }
        numChunks++;

        //Array that for each block, has all the logs of it
        ArrayList<StateLog>[] blockIdLogs = new ArrayList[numChunks];
        for(int i = 0; i<numChunks;i++){
            blockIdLogs[i] = new ArrayList<>();
        }
        Iterator fileIdLogsIterator = fileIdLogs.iterator();
        while (fileIdLogsIterator.hasNext()){
            StateLog log = (StateLog)fileIdLogsIterator.next();
            String[] tokens = log.getArgs().get(1).split("#");
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
                    String poolFileId = fileId+"#"+blockId;
                    try {
                        ManagedChannel poolChannel = ManagedChannelBuilder.forTarget(address).usePlaintext(true).build();
                        PoolServiceGrpc.PoolServiceBlockingStub poolStub = PoolServiceGrpc.newBlockingStub(poolChannel);
                        BlockID b = BlockID.newBuilder().setFileId(poolFileId).setBlockIndex(shardId).build();
                        ReadBlockRequest readBlockRequest = ReadBlockRequest.newBuilder().setBlockID(b).build();
                        BlockData blockData = poolStub.read(readBlockRequest);
                        shards[shardId] = blockData.getData().toByteArray();
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

    public static void loadConfig() throws IOException {
        config = new Properties();
        FileInputStream configFile = new FileInputStream("controller.config");
        config.load(configFile);
        configFile.close();
    }
}
