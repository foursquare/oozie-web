import java.io.File
import sbt.{config, AttributeKey, PathFinder, SettingKey, TaskKey}

final case class JettyClasspaths(classpath: PathFinder, jettyClasspath: PathFinder)

object WebKeys {
  val jettyConf = config("jetty") hide
  val webappPath = SettingKey[File]("webapp-path")
  val executableWar = TaskKey[Seq[(File, String)]]("executable-war")
  val temporaryWarPath = SettingKey[File]("temporary-war-path")
  val webappUnmanaged = SettingKey[PathFinder]("webapp-unmanaged")
  val prepareWebapp = TaskKey[Seq[(File, String)]]("prepare-webapp")
  val packageWar = TaskKey[File]("package-war")
  val jettyClasspaths = TaskKey[JettyClasspaths]("jetty-classpaths")
  val jettyWebappPath = SettingKey[File]("jetty-webapp-path")
  val jettyContext = SettingKey[String]("jetty-context")
  val jettyPort = SettingKey[Int]("jetty-port")

  val jettyRun = TaskKey[Unit]("jetty-run", "Start a Jetty")
  val jettyStop = TaskKey[Unit]("jetty-stop", "Stop a Jetty")
  val jettyReload = TaskKey[Unit]("jetty-reload", "Reload a Jetty")
}
