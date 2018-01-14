package pool;

import core.RegisterPoolRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.jgroups.JChannel;
import org.jgroups.Message;

import java.io.IOException;

import static org.jgroups.Message.Flag.RSVP;
import static org.jgroups.Message.Flag.RSVP_NB;

public class Pool {

    private Server server;
    private String address = "192.168.1.110";
    public static String dataDirectory = "data";

    public Pool() {
       this.server = ServerBuilder.forPort(0).addService(new PoolService()).build();
    }

    public static void main(String[] args) {
        Pool pool = new Pool();
        try {
            pool.start();
            dataDirectory += pool.server.getPort();
            pool.registerPool();
            pool.blockUntilShutdown();
        } catch (IOException e ) {
            System.out.println("Pool failed to start");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void registerPool() {
        JChannel channel = null;
        try {
            channel = new JChannel("tcp.xml");
            channel.setDiscardOwnMessages(true);
            channel.connect("ControllerCluster");
            RegisterPoolRequest registerPoolRequest = new RegisterPoolRequest(this.address + ":" + server.getPort());
            Message msg = new Message(null, registerPoolRequest);
            msg.setFlag(RSVP_NB);
            channel.send(msg);
            channel.disconnect();
            System.out.println("Register Sent");
        } catch (Exception e) {
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
