package client.cli;

import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.ArrayList;

public class UploadFileCommand implements Command {

    private String[] filePaths;
    private String endpoint = "file/upload";

    public UploadFileCommand(String[] filePaths) {
        this.filePaths = filePaths;
    }

    @Override
    public void run(WebTarget target) {
        for(String fileName : filePaths) {
            System.out.println("Uploading file " + fileName + " ...");
            File file = new File(fileName);
            InputStream fileInStream = null;
            try {
                fileInStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                System.out.println("File " + file + " doesn't exist");
                continue;
            }
            String contentDisposition = "attachment; filename=\"" + file.getName() + "\"";
            Response response = target.path(endpoint).path(file.getName())
                    .request(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .header("Content-Disposition", contentDisposition)
                    .post(Entity.entity(fileInStream, MediaType.APPLICATION_OCTET_STREAM_TYPE));
            System.out.println(response.getStatus());
        }


    }
}
