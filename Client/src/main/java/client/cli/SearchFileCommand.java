package client.cli;

import core.FileBean;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.ws.Endpoint;
import java.util.ArrayList;

public class SearchFileCommand implements Command {
    private String fileName;
    private String endpoint = "file/search";

    public SearchFileCommand(String fileName) {
        this.fileName = fileName;
    }


    @Override
    public void run(WebTarget target) {

        Response response = target.path(endpoint)
                .queryParam("pattern", fileName)
                .request(MediaType.APPLICATION_JSON)
                .get(Response.class);
        System.out.println(response.getStatus());
        ArrayList<FileBean> list = response.readEntity(new GenericType<ArrayList<FileBean>>(){});
        for (FileBean i : list) {
            System.out.println(i);
        }
    }
}
