package client.cli;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class UploadFileCommand implements Command {

    private String[] filePaths;
    private String endpoint = "file/upload";

    public UploadFileCommand(String[] filePaths) {
        this.filePaths = filePaths;
    }

    @Override
    public void run(WebTarget target) {
        for(String file : filePaths) {
            System.out.println("Uploading file " + file + " ...");
            File fileObj = new File(file);
            if(fileObj.isFile()) {
                FileDataBodyPart filePart = new FileDataBodyPart("file", fileObj);
                FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
                FormDataMultiPart multiPart = (FormDataMultiPart) formDataMultiPart.field("foo", "bar").bodyPart(filePart);
                Response response = target.path("file/upload")
                        .request()
                        .post(Entity.entity(multiPart, multiPart.getMediaType()));
                try {
                    formDataMultiPart.close();
                    multiPart.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("File " + file + " doesn't exist");
            }
        }


    }
}
