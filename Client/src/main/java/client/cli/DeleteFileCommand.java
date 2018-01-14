package client.cli;

import core.FileBean;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

public class DeleteFileCommand implements Command {
    private String[] fileNames;
    private String endpoint = "file/delete";

    public DeleteFileCommand(String[] fileNames) {
        this.fileNames = fileNames;
    }


    @Override
    public void run(WebTarget target) {
        for (String fileName : fileNames) {
            JsonObject body = Json.createObjectBuilder().add("file", fileName).build();
            Response response = target.path(endpoint)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(body.toString()));

            System.out.println(response.getStatus());
            if(response.getStatus() == 200){
                System.out.println("File deleted");
            } else if (response.getStatus() == 504){
                System.out.println("Error deleting file");
            }
        }
    }
}
