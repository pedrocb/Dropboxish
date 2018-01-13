package portal.file;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class RequestHandler extends Thread {
    private String id;
    private Server server;
    private int port;

    public RequestHandler(String id, byte[] fileData) {
        this.id = id;
        this.server = ServerBuilder.forPort(0).addService(new RequestHandlerService(id, fileData)).build();
        try{
            server.start();
            System.out.println("Request handler started on "+server.getPort());
        } catch (Exception e){
            e.printStackTrace();
        }
        this.port = server.getPort();
    }

    public int getPort() {
        return port;
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


    public void startServer() throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                RequestHandler.this.stop();
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
