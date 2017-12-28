import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.jgroups.*;


import static Helpers.ConsoleHelper.getTextFromConsole;

/*************
 *
 *  MAKE SURE YOU ARE USING IPV4 SINCE JGROUPS DEFAULTS TO IPV6 AND
 *  YOUR ROUTING TABLES ARE PROBABLY NOT CONFIGURED CORRECTLY
 *
 *  ADD THIS TO VM CONFIG
 *  ON EDIT CONFIGURATIONS -> VM OPTIONS
 *
 *  -Djava.net.preferIPv4Stack=true
 *
 *
 *
 */


public class Controller {
    private final String CLUSTER_NAME = "ControllerCluster";
    private JChannel channel;
    private ControllerReceiver receiver;
    private Server server;


    public static void main(String[] args) throws Exception{
        new Controller().start();

    }

    private void start() throws Exception {
        channel = new JChannel();
        channel.connect(CLUSTER_NAME);
        receiver = new ControllerReceiver();
        channel.setReceiver(receiver);

        server = ServerBuilder.forPort(0).addService(new ControllerService()).build();

        while(true){
            sendMessage();
        }
    }

    //send a message to all controllers (including himself)
    private void sendMessage(){
        try{
            String input = getTextFromConsole();
            Message msg = new Message(null,input);
            channel.send(msg);

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
