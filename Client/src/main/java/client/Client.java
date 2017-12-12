package client;

import org.eclipse.persistence.jaxb.rs.MOXyJsonProvider;
import org.glassfish.jersey.CommonProperties;


import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;


public class Client {
    public static void main(String[] args) {
        javax.ws.rs.client.Client httpclient = ClientBuilder.newClient()
                .property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true)
                .register(MOXyJsonProvider.class)
                .register(MultiPartFeature.class);
        String address = "http://localhost:9999";
        WebTarget target = httpclient.target(address);

        FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
        System.out.println("Client Running");
        /*Response response = target.path("file/download")
                .request(MediaType.APPLICATION_JSON)
                .post(); */
    }
}
