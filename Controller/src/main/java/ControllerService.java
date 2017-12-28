import core.ControllerServiceGrpc;
import core.Empty;
import core.PoolInfo;
import io.grpc.stub.StreamObserver;

public class ControllerService extends ControllerServiceGrpc.ControllerServiceImplBase {

    public ControllerService() {

    }

    @Override
    public void registerPool(PoolInfo request, StreamObserver<Empty> responseObserver) {
        super.registerPool(request, responseObserver);
    }
}
