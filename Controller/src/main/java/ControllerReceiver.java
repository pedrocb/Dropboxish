import com.google.protobuf.ByteString;
import core.*;
import com.backblaze.erasure.ReedSolomon;
import core.FileData;
import core.PortalServiceGrpc;
import core.RequestInfo;
import core.RequestReply;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/*
    Receiver is reponsible to implement fuctions that modulate a controller's behaviour
 */

public class ControllerReceiver extends ReceiverAdapter {
    ControllerState state = new ControllerState();

    public ControllerReceiver(ControllerState state) {
        this.state = state;
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized (state) {
            Util.objectToStream(state, new DataOutputStream(output));
        }
        System.out.println("getState Called");
        super.getState(output);
    }

    @Override
    public void setState(InputStream input) throws Exception {
        System.out.println("setState Called");
        ControllerState state = Util.objectFromStream(new DataInputStream(input));
        synchronized (this.state) {
            this.state.getPools().clear();
            this.state.getPools().addAll(state.getPools());
        }
        System.out.println(state);
    }

    public ControllerReceiver() {

    }

    //when a new request is received
    public void receive(Message msg) {
        JGroupRequest request = msg.getObject();
        System.out.println("got msg ");
        if(request.getType() == JGroupRequest.RequestType.UploadFile) {
            System.out.println("Got a upload file request from " + msg.getSrc());
            String requestId = request.getId();
            String address = request.getAddress();
            // Try to connect to portal, only the first will get the job
            System.out.println("Requesting job " + requestId + " to " + address);
            try {
                ManagedChannel portalChannel = ManagedChannelBuilder.forTarget(address).usePlaintext(true).build();
                PortalServiceGrpc.PortalServiceBlockingStub portalStub = PortalServiceGrpc.newBlockingStub(portalChannel);
                RequestInfo requestInfo = RequestInfo.newBuilder().setId(requestId).build();
                RequestReply requestReply = portalStub.handleRequest(requestInfo);
                Boolean gotTheJob = requestReply.getGotTheJob();
                System.out.println("Got the job " + requestId + ": " + gotTheJob);
                if (gotTheJob) {
                    uploadFileWork(requestId, portalStub);
                }
            } catch (Exception e) {
                System.out.println("it caput" + e.getMessage());
            }
        } else if (request.getType() == JGroupRequest.RequestType.RegisterPool){
            RegisterPoolRequest registerPoolRequest = (RegisterPoolRequest) request;
            System.out.println("Registering pool " + registerPoolRequest.getAddress());
            synchronized (state) {
                state.addPool(registerPoolRequest.getAddress());
            }
        } else {
            System.out.println("Got something else");
        }
    }

    private void uploadFileWork(String requestId, PortalServiceGrpc.PortalServiceBlockingStub stub){
        RequestInfo request = RequestInfo.newBuilder().setId(requestId).build();
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
    }

    private void fakeSomeWork(int requestId) {
        for (int i = 0; i < 10; i++) {
            System.out.println("Im working on " + requestId);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //when a new controller is added or removed!!
    public void viewAccepted(View view) {
        super.viewAccepted(view);
        System.out.println("New View :: " + view);
    }
}
