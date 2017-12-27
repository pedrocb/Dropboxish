package pool;

import core.*;
import io.grpc.stub.StreamObserver;

public class PoolService extends PoolServiceGrpc.PoolServiceImplBase {

    public PoolService() {

    }

    @Override
    public void write(WriteBlockRequest request, StreamObserver<StatusMsg> responseObserver) {
        super.write(request, responseObserver);
    }

    @Override
    public void read(ReadBlockRequest request, StreamObserver<BlockData> responseObserver) {
        super.read(request, responseObserver);
    }
}
