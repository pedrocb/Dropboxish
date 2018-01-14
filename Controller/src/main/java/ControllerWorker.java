import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class ControllerWorker extends Thread{
    private Server server;
    public int getPort() {
        return port;
    }

    private int port;

    public ControllerWorker(ControllerState state, String fileName, long timestamp){
        server = ServerBuilder.forPort(0).addService(new ControllerService(state, fileName, timestamp)).build();
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.port = server.getPort();
    }

    @Override
    public void run() {
        try {
            startServer();
            blockUntilShutdown();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void startServer () throws Exception{
        System.out.println("Listener started on port "+port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                ControllerWorker.this.stopServer();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stopServer() {
        if(server!=null)
            server.shutdown();
    }

    private void blockUntilShutdown() throws InterruptedException {
        if(server != null) {
            server.awaitTermination();
        }
    }
}
