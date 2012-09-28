// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.oozie.dashboard

import com.typesafe.config.ConfigFactory
import org.apache.oozie.client.{OozieClient, WorkflowAction, WorkflowJob}
import org.scalatra.ScalatraServlet
import org.scalatra.scalate.ScalateSupport
import scala.xml.XML
import scalaj.collection.Imports._
import java.text.SimpleDateFormat
import java.util.Date

object Implicits {
    implicit def prettyDate(d: Date): PrettyDate = new PrettyDate(d)
    implicit def sA(a: WorkflowAction): SuperAction = new SuperAction(a)
  }

class SuperAction(base: WorkflowAction) {
  def getUrl = {
    base.getExternalId match {
      case str: String if (str.contains("oozie")) => "/workflows/%s" format(str)
      case _ => base.getConsoleUrl
    }
  }
}

class PrettyDate(d: Date) {
  val formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
  def pp = {
    d match {
      case null => "<null>"
      case date => formatter.format(d);    
    }
  }
}

class OozieDashboard() extends ScalatraServlet with ScalateSupport {
  val conf = ConfigFactory.load("oozie")
  var oozie = new OozieClient(conf.getString("oozieUrl"))
  oozie.validateWSVersion()

  
  
  before() {
    contentType = "text/html"
  }

  def view(name: String) = "WEB-INF/views/%s" format(name)

  get("/") {    
    val filter = "status=RUNNING;status=PREP"
    val workflows = oozie.getJobsInfo(filter, 0, 1000).asScala.toList
    val coordinators = oozie.getCoordJobsInfo(filter, 0, 1000).asScala.toList
    ssp(view("index.ssp"), "workflows" -> workflows, "coordinators" -> coordinators)
  }

  get("/workflows") {
    val perPage = 50
    val offset = params.get("page").map(_.toInt).getOrElse(1)

    val start = (offset-1) * perPage
    val filter = null
    val jobs = oozie.getJobsInfo(filter, start, perPage).asScala.toList
    ssp(view("workflows/index.ssp"), "workflows" -> jobs, "page" -> offset)
  }

  get("/workflows/:id") {
    params.get("id") match {
      case Some(id) => {
        val workflow = oozie.getJobInfo(id, 0, 1000)
        val definition = oozie.getJobDefinition(workflow.getId)
        val xml = XML.loadString(workflow.getConf)
        val userProps: List[String] = (xml \\ "property").map{node =>
          val name = (node \\ "name").text
          val value = (node \\ "value").text.replaceAll("\n", " ")
          name match {
            case s: String if (!s.contains(".")) => Some("%s=%s".format(s, value))
            case _ => None
          }
        }.flatten.toList
        ssp(view("workflows/show.ssp"), "workflow" -> workflow, "definition" -> definition, "props" -> userProps)
      }
      case _ => halt(404)
    }
  }

  get("/workflows/:id/log") {
    contentType = "text/plain"
    params.get("id") match {
      case Some(id) => {
        oozie.getJobLog(id)
      }
      case _ => halt(404)
    }
  }

  get("/coordinators") {
    val perPage = 1000
    val jobs = oozie.getCoordJobsInfo(null, 0, 1000).asScala.toList
    ssp(view("coordinators/index.ssp"), "jobs" -> jobs)
  }

  get("/coordinators/:id") {
    params.get("id") match {
      case Some(id) => {
        val job = oozie.getCoordJobInfo(id, 0, 1000)
        val definition = oozie.getJobDefinition(job.getId)
        ssp(view("coordinators/show.ssp"), "job" -> job, "definition" -> definition)
      }
      case _ => halt(404)
    }
  }

  get("/coordinators/:id/lastactionstatus") {
    contentType = "text/plain"
    params.get("id") match {
      case Some(id) => {
        val job = oozie.getCoordJobInfo(id, 0, 1000)
        job.getActions.asScala.last.getStatus.toString
      }
      case _ => halt(404)
    }
    
  }

  post("/coordinators/:id/:action/rerun") {
    params.get("id") match {
      case Some(id) => params.get("action") match {
        case Some(action) => {
          println("running %s for job %s" format(action, id))
          oozie.reRunCoord(id, "action",action, false, false)
          redirect("/coordinators/%s".format(id))
        }
        case _ => halt(404)
      }
      case _ => halt(404)
    }
  }

  get("/search") {
    params.get("q") match {
      case Some(query) => {
        val workflows = oozie.getJobsInfo(null, 0, 500).asScala.filter(_.getAppName.contains(query))
        val coordinators = oozie.getCoordJobsInfo(null, 0, 500).asScala.filter(_.getAppName.contains(query))
        ssp(view("index.ssp"), 
          "workflows" -> workflows, 
          "coordinators" -> coordinators, 
          "title" -> "Jobs containing '%s'".format(query),
          "viewMore" -> false
          )
      }
      case _ => redirect("/")
    }
  }


}