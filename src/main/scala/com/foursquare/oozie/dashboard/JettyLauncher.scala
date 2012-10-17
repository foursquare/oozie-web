// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.oozie.dashboard

import org.eclipse.jetty.server.{AbstractHttpConnection, Handler, Request, Server}
import org.eclipse.jetty.server.handler.{AbstractHandler, HandlerList}
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
import java.net.URL
import java.security.ProtectionDomain
import java.util.Date
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

object JettyLauncher { 
  def main(args: Array[String]): Unit = {
    val port = args(0).toInt
    val warPath = args(1)
    val contextPath = args(2)
    jettyRun(port, warPath, contextPath)
  }

  def jettyRun(
      port: Int,
      warPath: String,
      contextPath: String): Server = {
    val DefaultMaxIdleTime = 30000

    val connector = {
      val c = new SelectChannelConnector
      c.setPort(port)
      c.setMaxIdleTime(DefaultMaxIdleTime)
      c
    }

    val server = {
      val s = new Server
      s.addConnector(connector)
      s
    }

    val stopHandler = new AbstractHandler {
      override def handle(
          target: String,
          baseRequest: Request,
          request: HttpServletRequest,
          response: HttpServletResponse) {
        if (target == "/jetty-stop") {
          val s = getServer
          if (s != null)
            s.stop()
        }
      }
    }

    val webapp = new WebAppContext(warPath, contextPath)

    val handlerList = new HandlerList
    handlerList.setHandlers(Array[Handler](stopHandler, webapp))

    server.setHandler(handlerList)
    server.start()
    server
  }
}
