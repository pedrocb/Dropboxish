package portal;

import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Created by up201602876 on 11/16/17.
 */
public class Portal {

   public static void main(String[] args) {
      int port = 9999;
      Server jettyServer = new Server(port);
      ServletContextHandler context = new ServletContextHandler(jettyServer, "/*");
      System.out.println("Portal running");
   }

}
