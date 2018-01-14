package portal.file;

import core.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.internal.IoUtils;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.sql.Time;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Path("file")
public class FileService {
    @POST
    @Path("download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(String request) throws FileNotFoundException {
        JsonObject fileObject;
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(request));
            fileObject = jsonReader.readObject();
        } catch (Exception e) {
            return Response.status(402).build();
        }
        String fileName = fileObject.getString("file");
        ReentrantLock lock = new ReentrantLock();
        RequestHandlerService service = new RequestHandlerService(null, Thread.currentThread());
        Server server = ServerBuilder.forPort(0).addService(service).build();
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sendMessage(new DownloadFileRequest(fileName, "localhost:" + server.getPort()));
        try {
            if(lock.tryLock(10, TimeUnit.SECONDS)) {
                //A controller responded

            } else {
                server.shutdown();
                //No controller responded in 10 seconds
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        File file = new File("filesReceived/" + fileName);
        if (file.exists()) {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            StreamingOutput streamingOutput = out -> {
                byte[] fileBytes = new byte[1024];
                int read;
                while ((read = in.read(fileBytes)) != -1) {
                    try {
                        out.write(fileBytes, 0, read);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    out.flush();
                }
                out.close();
            };
            return Response.status(200).entity(streamingOutput).build();
        } else {
            return Response.status(404).build();
        }
    }

    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listFiles() {
        ReentrantLock lock = new ReentrantLock();
        return Response.status(200).build();
    }

    @GET
    @Path("search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchFiles(@QueryParam("pattern") final String pattern) {
        return Response.status(200).build();
    }

    @POST
    @Path("upload/{fileName}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response uploadFile(@PathParam("fileName") String fileName, InputStream fileInputStream) {
        try {
            int read;
            byte[] bytes = new byte[1024];
            OutputStream out = new FileOutputStream(new File("filesReceived/" + fileName));
            while ((read = fileInputStream.read(bytes, 0, 1024)) != -1) {
                System.out.println("Read " + read + " bytes.");
                out.write(bytes, 0, read);
            }
            out.flush();
            out.close();
            //TODO make data [] available right when received
            byte[] data = IoUtils.toByteArray(new FileInputStream(new File("filesReceived/" + fileName)));
            System.out.println("Data length " + data.length);

            RequestHandlerService service = new RequestHandlerService(data, Thread.currentThread());
            Server server = ServerBuilder.forPort(0).addService(service).build();
            server.start();
            UploadFileRequest request = new UploadFileRequest(fileName, "localhost:"+server.getPort());
            sendMessage(request);

            boolean waiting = true;
            while (waiting) {
                System.out.println("Waiting for controller response");
                long tBefore = System.currentTimeMillis();
                synchronized (Thread.currentThread()) {
                    Thread.currentThread().wait(1000 * 10);
                    System.out.println("Wait stopped");
                }
                long timePassed = (System.currentTimeMillis() - tBefore);
                System.out.println(timePassed);
                if (timePassed >= 10000) {
                    System.out.println("Did not get notified so no controller responded");
                    waiting = false;
                    //No controller responded in 10 seconds
                } else {
                    System.out.println("Got notified... Waiting for response");
                    //A controller responded
                    synchronized (service) {
                        if (service.getResponse() == null) {
                            System.out.println("Did not have a response");
                        } else {
                            System.out.println(service.getResponse());
                            waiting = false;
                        }
                    }
                }
            }

            server.awaitTermination();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return Response.status(200).build();
    }

    private void sendMessage(JGroupRequest request) {
        try {
            JChannel channel = new JChannel("tcp.xml");
            channel.connect("ControllerCluster");
            channel.send(new Message(null, request));
            TimeUnit.SECONDS.sleep(0);
            channel.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
