import sbt._
import sbt.classpath.ClasspathUtilities
import sbt.Keys._
import WebKeys._

object WebDefaults {
  lazy val allSettings = webSettings ++ defaultSettings

  val webSettings: Seq[Project.Setting[_]] = Seq(
    ivyConfigurations += jettyConf,
    temporaryWarPath <<= (target){ (target) => target / "webapp" },
    webappUnmanaged := PathFinder.empty,
    configuration in packageWar := Compile,
    artifact in packageWar <<= name(n => Artifact(n, "war", "war")),
    managedClasspath in jettyClasspaths <<= (classpathTypes, update) map { (ct, report) => Classpaths.managedJars(jettyConf, ct, report) },
    jettyClasspaths <<= (fullClasspath in Runtime, managedClasspath in jettyClasspaths) map { (cp, jettyCp) =>
      JettyClasspaths(cp.map(_.data), jettyCp.map(_.data))
    },
    jettyWebappPath <<= temporaryWarPath,
    jettyContext := "/"
  )

  lazy val defaultSettings: Seq[Setting[_]] = Seq(
    jettyPort := {
      try {
        System.getProperty("jetty.port").toInt
      } catch {
        case e: Exception => 8080
      }
    },
    libraryDependencies ++= Seq(
      "javax.servlet"     % "servlet-api"     % "2.5"             % "provided",
      "org.eclipse.jetty" % "jetty-webapp"    % "7.6.7.v20120910" % "runtime,test,provided,jetty"
    ),
    webappPath <<= (sourceDirectory in Compile)(_ / "webapp"),
    mainClass in packageWar := Some("StartJetty"),
    packageOptions in packageWar <<= (packageOptions, mainClass in packageWar) map { (p, main) =>
      main.map(Package.MainClass.apply) ++: p
    },
    executableWar <<=
      (externalDependencyClasspath in Compile, temporaryWarPath, classDirectory in Compile,
          copyResources in Runtime) map {
        (deps, warPath, classDir, _) =>
          executableWarAction(deps, warPath, classDir)
      },
    jettyRun <<=
      (jettyPort, webappPath, jettyContext, runner, fullClasspath in Runtime, streams) map {
        (port, warPath, contextPath, r, cp, s) =>
          val args = Seq(port.toString, warPath.toString, contextPath)
          r.run("com.foursquare.oozie.dashboard.JettyLauncher", cp.files, args, s.log)
      },
    jettyStop <<=
      (jettyPort) map { port =>
        val url = new java.net.URL("http://localhost:" + port + "/jetty-stop")
        val c = url.openConnection
        try {
          c.getInputStream
        } catch {
          case e: java.net.SocketException => ()
        }
        ()
      },
    jettyReload <<= (jettyRun, jettyStop) map { (_, _) => () }
  ) ++ Defaults.packageTasks(packageWar, packageWarTask)

  def packageWarTask: Project.Initialize[Task[Seq[(File, String)]]] =
    (copyResources in Runtime, webappPath, temporaryWarPath, jettyClasspaths, webappUnmanaged,
        excludeFilter in prepareWebapp, streams, executableWar) map {
      (r, w, wp, cp, wu, excludes, s, ew) =>
        packageWarTask(w, wp, cp.classpath, wu, excludes, ew, s.log)
    }

  def packageWarTask(
      webappPath: File,
      warPath: File,
      classpath: PathFinder,
      ignore: PathFinder,
      defaultExcludes: FileFilter,
      executableWarFiles: Seq[(File, String)],
      slog: Logger): Seq[(File, String)] = {
    val log = slog.asInstanceOf[AbstractLogger]

    val webInfPath = warPath / "WEB-INF"
    val webLibPath = webInfPath / "lib"
    val classesPath = webInfPath / "classes"

    val (libs, directories) = classpath.get.toList.partition(ClasspathUtilities.isArchive)

    val webappContents = (webappPath ***) --- (webappPath ** defaultExcludes)

    val webappFiles = webappContents x (relativeTo(webappPath) | flat)
    val libFiles = libs x flatRebase("WEB-INF/lib")
    val classesFiles =
      (for (dir <- directories) yield {
        ((dir ***) --- (dir ** defaultExcludes)) x Path.rebase(dir, "WEB-INF/classes")
      }).flatten

    webappFiles ++ libFiles ++ classesFiles ++ executableWarFiles
  }

  def executableWarAction(
      externalDependencies: Classpath,
      temporaryWarPath: File,
      mainCompilePath: File): Seq[(File, String)] = {
    val jars =
      (externalDependencies
        .map(_.data)
        .filter(file => file.toString.contains("jetty") || file.toString.contains("servlet-api")))
    val unzipped = jars.flatMap(jar => IO.unzip(jar, temporaryWarPath))
    IO.delete(temporaryWarPath / "META-INF")
    val startJettyClasses = (mainCompilePath ** "StartJetty*")
    val startClasses = IO.copy(startJettyClasses x Path.flat(temporaryWarPath))
    (unzipped ++ startClasses) x (relativeTo(temporaryWarPath) | flat)
  }
}
