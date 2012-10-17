// Copyright 2012 Foursquare Inc. All Rights Reserved.

import java.net.URL;
import java.security.ProtectionDomain;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

public class StartJetty {
  public static void main(String [] args) throws Exception {
    int port = Integer.valueOf(System.getProperty("jetty.port", "8080"));

    final Server server = new Server(port);

    ProtectionDomain protectionDomain = StartJetty.class.getProtectionDomain();
    URL location = protectionDomain.getCodeSource().getLocation();

    WebAppContext context = new WebAppContext();
    context.setContextPath("/");
    context.setDescriptor(location.toExternalForm() + "/WEB-INF/web.xml");
    context.setServer(server);
    context.setWar(location.toExternalForm());

    server.setHandler(context);

    server.start();
    server.join();
  }
}
