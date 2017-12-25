package client.cli;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;

public class DownloadFileCommand implements Command {

    private String[] fileNames;
    private String endpoint = "file/download";

    public DownloadFileCommand(String[] fileNames) {
        this.fileNames = fileNames;
    }

    @Override
    public void run(WebTarget target) {
        for (String fileName : fileNames) {
            JsonObject body = Json.createObjectBuilder().add("file", fileName).build();

            Response response = target.path(endpoint)
                    .request(MediaType.APPLICATION_OCTET_STREAM)
                    .post(Entity.json(body.toString()));
            if (response.getStatus() == 200) {
                try {
                    OutputStream fileOutputStream = new FileOutputStream("downloadFiles/" + fileName);
                    InputStream fileInputStream = response.readEntity(InputStream.class);
                    writeFile(fileInputStream, fileOutputStream);
                    System.out.println("File " + fileName + " downloaded.");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("File " + fileName + " not found.");
            }
        }
    }

    public static void writeFile(InputStream fileInputStream, OutputStream outputStream) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            fileInputStream.close();
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            outputStream.close();
        }

    }
}
