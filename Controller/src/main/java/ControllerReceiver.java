import com.google.protobuf.ByteString;
import core.*;
import com.backblaze.erasure.ReedSolomon;
import core.PortalServiceGrpc;
import core.RequestInfo;
import core.RequestReply;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import java.io.*;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;


/*
    Receiver is reponsible to implement fuctions that modulate a controller's behaviour
 */

public class ControllerReceiver extends ReceiverAdapter {
    ControllerState state = new ControllerState();
    Lock lock;
    private JChannel channel;

    public ControllerReceiver(ControllerState state, JChannel channel) {
        this.state = state;
        this.lock = lock;
        this.channel = channel;
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
            this.state.getLogs().clear();
            this.state.getLogs().addAll(state.getLogs());
        }
        System.out.println(state);
    }

    public ControllerReceiver() {

    }

    //when a new request is received
    public void receive(Message msg) {
        System.out.println("got msg ");
        JGroupRequest request = msg.getObject();
        new ReceiverThread(request, lock, state, channel).start();
        System.out.println("Leaving receive");
    }


    //when a new controller is added or removed!!
    public void viewAccepted(View view) {
        super.viewAccepted(view);
        System.out.println("New View :: " + view);
    }

    public void setLock(Lock lock) {
        this.lock = lock;
    }

}
