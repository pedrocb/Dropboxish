package portal.file;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.nio.file.Files;

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
        if(file.exists()) {
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
            while((read = fileInputStream.read(bytes, 0, 1024)) != -1) {
                System.out.println("Read " + read + " bytes.");
                out.write(bytes, 0, read);
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Response.status(200).build();
    }
}
