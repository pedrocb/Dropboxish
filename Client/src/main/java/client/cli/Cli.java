package client.cli;

import org.eclipse.persistence.jaxb.rs.MOXyJsonProvider;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import sun.net.www.http.HttpClient;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.util.Scanner;

public class Cli {

    private boolean running = false;
    private WebTarget target;
    private Client httpClient;

    public Cli() {
        this.httpClient = ClientBuilder.newClient()
                .property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true)
                .register(MOXyJsonProvider.class)
                .register(MultiPartFeature.class);
        String address = "http://localhost:9999";
        target = httpClient.target(address);
    }

    public void start() {
        running = true;
        Scanner scanner = new Scanner(System.in);
        String input;
        Command command;
        while(running) {
            System.out.println("Commands:");
            System.out.println("upload <file>");

            input = scanner.nextLine();
            if(input.startsWith("upload ")) {
                String[] filenames = input.replace("upload ", "").split(" ");
                command = new UploadFileCommand(filenames);
                command.run(target);
            }

        }
    }
}