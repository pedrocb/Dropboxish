package portal;

import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.net.SocketException;

/**
 * Created by up201602876 on 11/16/17.
 */
public class Portal {

   public static void main(String[] args) {
      int port = 9999;
      Server jettyServer = new Server(port);
      ResourceConfig resourceConfig = new ResourceConfig()
              .packages("portal")
              .property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true)
              .register(MoxyJsonFeature.class)
              .register(MultiPartFeature.class);
      ServletContextHandler context = new ServletContextHandler(jettyServer, "/*");

      context.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/*");
      NCSARequestLog requestLog = new NCSARequestLog();
      requestLog.setAppend(true);
      requestLog.setExtended(false);
      requestLog.setLogTimeZone("GMT");
      requestLog.setLogLatency(true);
      requestLog.setRetainDays(90);

      jettyServer.setRequestLog(requestLog);

      try {
         jettyServer.start();
         System.out.println("REST API started..");
         jettyServer.join();
      } catch (SocketException e) {
         System.out.println("Port already in use");
      } catch (Exception e) {
         e.printStackTrace();
         try {
            jettyServer.stop();
         } catch (Exception e1) {
            e1.printStackTrace();
         }
         jettyServer.destroy();
      }
   }

}
