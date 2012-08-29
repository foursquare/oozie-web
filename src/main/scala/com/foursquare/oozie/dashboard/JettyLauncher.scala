package com.foursquare.oozie.dashboard

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import org.scalatra.ScalatraServlet
import org.scalatra.scalate.ScalateSupport
import scalaj.collection.Imports._
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import java.text.SimpleDateFormat
import java.util.Date

object JettyLauncher { 
  def main(args: Array[String]) {
    if (args.size < 1) {
      println("usage: ./oozieDash <port>")
      return
    }
    val port = args(0).toInt
    val server = new Server(port)

    val static = new ResourceHandler()
    static.setResourceBase("src/main/webapp/static")
    static.setDirectoriesListed(false)

    val staticContext = new ContextHandler()
    staticContext.setContextPath("/static");
    staticContext.setHandler(static);

    val root = new ServletContextHandler(ServletContextHandler.SESSIONS)
    root.setContextPath("/")
    root.addServlet(new ServletHolder(new OozieDashboard()), "/*")
    root.setResourceBase("src/main/webapp")


    val handlers = new HandlerList()
    handlers.setHandlers(Array(staticContext, root))
    server.setHandler(handlers)

    server.start()
    server.join()
  }
}