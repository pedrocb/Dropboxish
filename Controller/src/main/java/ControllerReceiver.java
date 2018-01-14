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
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import java.io.*;
import java.nio.ByteBuffer;
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

    public ControllerReceiver(ControllerState state) {
        this.state = state;
        this.lock = lock;
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
        System.out.println("got msg ");
        JGroupRequest request = msg.getObject();
        new ReceiverThread(request, lock, state).start();
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

    public void setLock(Lock lock) {
        this.lock = lock;
    }

}
