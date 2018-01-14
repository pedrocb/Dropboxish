package portal.file;

import com.google.common.collect.Lists;
import core.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.internal.IoUtils;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.*;
import java.sql.Time;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jgroups.Message.Flag.RSVP_NB;

@Path("file")
public class FileService {
    @POST
    @Path("delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFile(String request) throws FileNotFoundException {
        JsonObject fileObject;
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(request));
            fileObject = jsonReader.readObject();
        } catch (Exception e) {
            return Response.status(402).build();
        }
        String fileName = fileObject.getString("file");
        System.out.println("Deleting " + fileName);
        return Response.status(200).build();
    }

    @POST
    @Path("download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(String request) throws FileNotFoundException, InterruptedException {
        JsonObject fileObject;
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(request));
            fileObject = jsonReader.readObject();
        } catch (Exception e) {
            return Response.status(402).build();
        }
        String fileName = fileObject.getString("file");
        DownloadFileService service = new DownloadFileService(Thread.currentThread());
        Server server = ServerBuilder.forPort(0).addService(service).build();
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sendMessage(new DownloadFileRequest(fileName, "localhost:" + server.getPort()));
        boolean waiting = true;
        byte[] fileData = null;
        Response response = Response.status(504).build();
        while (waiting) {
            synchronized (service) {
                if (service.getFile() == null) {
                    System.out.println("Did not have a response");
                } else {
                    fileData = service.getFile();
                    ByteArrayInputStream in = new ByteArrayInputStream(fileData);
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
                    response = Response.status(200).entity(streamingOutput).build();
                    waiting = false;
                    break;
                }
            }
            long tBefore = System.currentTimeMillis();
            synchronized (Thread.currentThread()) {
                System.out.println("Waiting");
                Thread.currentThread().wait(1000 * 10);
                System.out.println("Wait stopped");
            }
            long timePassed = (System.currentTimeMillis() - tBefore);
            System.out.println(timePassed);
            if (timePassed >= 10000) {
                System.out.println("Did not get notified so no controller responded");
                response = Response.status(503).build();
                waiting = false;
                //No controller responded in 10 seconds
            } else {
                System.out.println("Got notified... Waiting for response");
                //A controller responded
            }
        }
        return response;
    }

    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listFiles() {
        System.out.println("Listing files");
        ListFileService service = new ListFileService(Thread.currentThread());
        Server server = ServerBuilder.forPort(0).addService(service).build();
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sendMessage(new ListFilesRequest("192.168.1.114:" + server.getPort()));
        boolean waiting = true;
        Response response = Response.status(504).build();
        while (waiting) {
            synchronized (service) {
                if (service.getFilesInfo()== null) {
                    System.out.println("Did not have a response");
                } else {
                    ArrayList<FileInfo> filesInfos = service.getFilesInfo();
                    ArrayList<FileBean> result = new ArrayList<>();
                    for(FileInfo fileInfo : filesInfos) {
                        result.add(new FileBean(fileInfo.getFileName(), fileInfo.getFileSize()));
                    }
                    GenericEntity<ArrayList<FileBean>> entity
                            = new GenericEntity<ArrayList<FileBean>>(Lists.newArrayList(result)) {};
                    response = Response.status(200).entity(entity).build();
                    waiting = false;
                    break;
                }
            }
            long tBefore = System.currentTimeMillis();
            synchronized (Thread.currentThread()) {
                System.out.println("Waiting");
                try {
                    Thread.currentThread().wait(1000 * 10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Wait stopped");
            }
            long timePassed = (System.currentTimeMillis() - tBefore);
            System.out.println(timePassed);
            if (timePassed >= 10000) {
                System.out.println("Did not get notified so no controller responded");
                response = Response.status(503).build();
                waiting = false;
                //No controller responded in 10 seconds
            } else {
                System.out.println("Got notified... Waiting for response");
                //A controller responded
            }
        }
        return response;
    }

    @GET
    @Path("search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchFiles(@QueryParam("pattern") final String pattern) {
        System.out.println("Searching " + pattern);
        ListFileService service = new ListFileService(Thread.currentThread());
        Server server = ServerBuilder.forPort(0).addService(service).build();
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sendMessage(new ListFilesRequest("192.168.1.114:" + server.getPort()));
        boolean waiting = true;
        Response response = Response.status(504).build();
        while (waiting) {
            synchronized (service) {
                if (service.getFilesInfo()== null) {
                    System.out.println("Did not have a response");
                } else {
                    ArrayList<FileInfo> filesInfos = service.getFilesInfo();
                    ArrayList<FileBean> result = new ArrayList<>();
                    Pattern patternObj = Pattern.compile(pattern);
                    for(FileInfo fileInfo : filesInfos) {
                        Matcher matcher = patternObj.matcher(fileInfo.getFileName());
                        if(matcher.matches()) {
                            result.add(new FileBean(fileInfo.getFileName(), fileInfo.getFileSize()));
                        }
                    }
                    GenericEntity<ArrayList<FileBean>> entity
                            = new GenericEntity<ArrayList<FileBean>>(Lists.newArrayList(result)) {};
                    response = Response.status(200).entity(entity).build();
                    waiting = false;
                    break;
                }
            }
            long tBefore = System.currentTimeMillis();
            synchronized (Thread.currentThread()) {
                System.out.println("Waiting");
                try {
                    Thread.currentThread().wait(1000 * 10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Wait stopped");
            }
            long timePassed = (System.currentTimeMillis() - tBefore);
            System.out.println(timePassed);
            if (timePassed >= 10000) {
                System.out.println("Did not get notified so no controller responded");
                response = Response.status(503).build();
                waiting = false;
                //No controller responded in 10 seconds
            } else {
                System.out.println("Got notified... Waiting for response");
                //A controller responded
            }
        }
        return response;
    }

    @POST
    @Path("upload/{fileName}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response uploadFile(@PathParam("fileName") String fileName, InputStream fileInputStream) {
        Response response = Response.status(504).build();
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
            UploadFileRequest request = new UploadFileRequest(fileName, "192.168.1.114:"+server.getPort());
            sendMessage(request);
            boolean waiting = true;
            while (waiting) {
                synchronized (service) {
                    if (service.getResponse() == null) {
                        System.out.println("Did not have a response");
                    } else {
                        response = service.getResponse();
                        waiting = false;
                        break;
                    }
                }
                long tBefore = System.currentTimeMillis();
                synchronized (Thread.currentThread()) {
                    System.out.println("Waiting");
                    Thread.currentThread().wait(1000 * 10);
                    System.out.println("Wait stopped");
                }
                long timePassed = (System.currentTimeMillis() - tBefore);
                System.out.println(timePassed);
                if (timePassed >= 10000) {
                    System.out.println("Did not get notified so no controller responded");
                    waiting = false;
                    response = Response.status(503).build();
                    //No controller responded in 10 seconds
                } else {
                    System.out.println("Got notified... Waiting for response");
                    //A controller responded
                }
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return response;
    }

    private void sendMessage(JGroupRequest request) {
        try {
            JChannel channel = new JChannel("tcp.xml");
            channel.setDiscardOwnMessages(true);
            channel.connect("ControllerCluster");
            Message msg = new Message(null, request);
            msg.setFlag(RSVP_NB);
            /*
            System.out.println("Sending message");
            System.out.println(channel.getViewAsString());
            System.out.println(channel.getDiscardOwnMessages());
            */
            channel.send(msg);
            System.out.println("Disconnecting");
            channel.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
