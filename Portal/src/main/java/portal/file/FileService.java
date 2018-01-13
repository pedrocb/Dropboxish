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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Path("file")
public class FileService {
    @POST
    @Path("download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(String request) throws IOException {
        JsonObject fileObject;
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(request));
            fileObject = jsonReader.readObject();
        } catch (Exception e) {
            return Response.status(402).build();
        }
        String fileName = fileObject.getString("file");
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

            ReentrantLock lock = new ReentrantLock();
            RequestHandlerService service = new RequestHandlerService(data, lock);
            Server server = ServerBuilder.forPort(0).addService(service).build();
            server.start();
            if(lock.tryLock(10, TimeUnit.SECONDS)) {
                //A controller responded

            } else {
                server.shutdown();
                //No controller responded in 10 seconds
            }
            UploadFileRequest request = new UploadFileRequest(fileName);
            sendMessage(request);
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
            MessageDispatcher messageDispatcher = new MessageDispatcher(channel);
            channel.send(new Message(null, request));
            //Sometimes the message drops because we disconnect
            //TODO: make send msg async? not matter if this leaves
            TimeUnit.SECONDS.sleep(1);
            channel.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int generateId() {
        return (int) Math.floor(Math.random() * Integer.MAX_VALUE);
    }
}
