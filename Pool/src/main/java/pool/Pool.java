package pool;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class Pool {

    private Server server;

    public Pool() {
       this.server = ServerBuilder.forPort(0).addService(new PoolService()).build();
    }

    public static void main(String[] args) {
        Pool pool = new Pool();
        try {
            pool.start();
            pool.blockUntilShutdown();
        } catch (IOException e ) {
            System.out.println("Pool failed to start");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if(server != null) {
            server.awaitTermination();
        }
    }

    public void start() throws IOException {
        server.start();
        System.out.println("Storage pool on");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                Pool.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if(server!=null) {
            server.shutdown();
        }
    }
}
