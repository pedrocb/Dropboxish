package client.cli;

import core.FileBean;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

public class ListFilesCommand implements Command {
    private String endpoint = "file/list";

    public ListFilesCommand() {
    }


    @Override
    public void run(WebTarget target) {

        Response response = target.path(endpoint)
                .request(MediaType.APPLICATION_JSON)
                .get(Response.class);
        System.out.println(response.getStatus());
        ArrayList<FileBean> list = response.readEntity(new GenericType<ArrayList<FileBean>>(){});
        for (FileBean i : list) {
            System.out.println(i);
        }
    }
}
