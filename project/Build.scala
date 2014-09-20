import com.typesafe.sbt.less.Import.LessKeys
import com.typesafe.sbt.web
import com.typesafe.sbt.web.SbtWeb
import org.sbtidea.SbtIdeaPlugin
import sbt._
import Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._

object Build extends sbt.Build {
  import web.Import._


  val handleLootyHtml = Def.task {
    val css = """
  <link rel="stylesheet" href="jslib/slickgrid/css/smoothness/jquery-ui-1.8.16.custom.css"/>
  <link rel="stylesheet" href="jslib/slickgrid/slick.grid.css"/>
  <link rel="stylesheet" href="jslib/font-awesome-4.2.0/css/font-awesome.css"/>
  <link rel="stylesheet" href="jslib/select2/select2.css">
    """

    val jslib = """
<script type="text/javascript" src="jslib/d3.js"></script>
<script type="text/javascript" src="jslib/jquery.js"></script>
<script type="text/javascript" src="jslib/jquery-migrate.js"></script>
<script type="text/javascript" src="jslib/react-with-addons.js"></script>
<script type="text/javascript" src="jslib/signals.js"></script>
<script type="text/javascript" src="jslib/crossroads.js"></script>
<script type="text/javascript" src="jslib/hasher.js"></script>
<script type="text/javascript" src="jslib/mespeak/mespeak.full.js"></script>
<script type="text/javascript" src="jslib/esprima.js"></script>
<script type="text/javascript" src="jslib/vm.js"></script>
<script type="text/javascript" src="jslib/ace/ace.js" charset="utf-8"></script>
<script type="text/javascript" src="jslib/select2/select2.js"></script>
<script type="text/javascript" src="jslib/filesaver.js"></script>
<script type="text/javascript" src="jslib/slickgrid/lib/jquery.event.drag-2.0.min.js"></script>
<script type="text/javascript" src="jslib/slickgrid/lib/jquery.event.drop-2.0.min.js"></script>
<script type="text/javascript" src="jslib/slickgrid/slick.core.js"></script>
<script type="text/javascript" src="jslib/slickgrid/slick.grid.js"></script>
<script type="text/javascript" src="jslib/slickgrid/slick.dataview.js"></script>
    """

    val analytics  = """
<script type="text/javascript" src="jslib/analytics.js"></script>
<script src="https://ssl.google-analytics.com/ga.js" async="true"></script>
<script type="text/javascript" src="jslib/startup.js"></script>
    """

    //Copy over the html files, while filling in the template sections
    val lessFiles = {
      val i = (includeFilter in (Assets, LessKeys.less)).value
      val x = (excludeFilter in (Assets, LessKeys.less)).value
      val srcDir = (sourceDirectory in Assets).value
      val lessSrcs = (sourceDirectory in Assets).value ** (i -- x)
      val f = lessSrcs.pair(relativeTo(srcDir))
      f.map(_._2).map(_.replace(".less", ".css"))
    }

    val less = lessFiles.map(f=>s"""<link rel="stylesheet" href="$f"/>""").mkString("\n")

    val htmlSrcDir: File = (sourceDirectory in Assets).value
    val htmlSrcs: PathFinder = htmlSrcDir * "*.template.html"
    val outDir: File = WebKeys.webTarget.value / "public" / "main"
    val outMappings = htmlSrcs pair Path.rebase(htmlSrcDir, outDir)
    outMappings.flatMap {
      case (in, out0) =>
        val content = IO.read(in)
        val outDir = out0.getParentFile
        val regex = """.*/([^/]+)\.template\.html""".r
        val basename = regex.findFirstMatchIn(in.getPath).get.group(1)
        //Make a dev version and a release version
        val devFile = outDir / (basename + "-dev.html")
        val releaseFile = outDir / (basename + ".html")

        val devScripts = """<script type="text/javascript" src="looty-fastopt.js"></script>""".stripMargin
        val releaseScripts = """<script type="text/javascript" src="looty-opt.js"></script>"""

        def addAll(sjsScripts : String) : String = {
          var res = content
          res = res.replace("<!-- insert scalajs -->", sjsScripts)
          res = res.replace("<!-- insert css -->", css)
          res = res.replace("<!-- insert less -->", less)
          res = res.replace("<!-- insert jslib -->", jslib)
          res = res.replace("<!-- insert analytics -->", analytics)
          res
        }

        val devOut = addAll(sjsScripts = devScripts)
        val releaseOut = addAll(sjsScripts = releaseScripts)

        IO.write(devFile, devOut.getBytes("UTF-8"))
        IO.write(releaseFile, releaseOut.getBytes("UTF-8"))
        List(devFile, releaseFile)
    }

    Seq[File](outMappings.map(_._1): _*)
  }


  lazy val sjsTasks = List(
    ScalaJSKeys.packageExternalDepsJS,
    ScalaJSKeys.packageInternalDepsJS,
    ScalaJSKeys.packageExportedProductsJS,
    ScalaJSKeys.fastOptJS,
    ScalaJSKeys.fullOptJS)

  lazy val sjsOutDir = Def.settingKey[File]("directory for javascript files output by scalajs")

  lazy val looty: Project = Project("looty", file("looty")).settings(
    scalaJSSettings: _*).settings(
      scalacOptions += "-deprecation",
      scalacOptions += "-unchecked",
      scalacOptions += "-feature",
      scalacOptions += "-language:implicitConversions",
      scalacOptions += "-language:existentials",
      scalacOptions += "-language:higherKinds"
    ).settings(
      cgta.otest.OtestPlugin.settingsSjs: _*
    ).settings(
      autoCompilerPlugins := true,
      ScalaJSKeys.requiresDOM := true,
      libraryDependencies += compilerPlugin("org.scala-lang.plugins" % "continuations" % scalaVersion.value),
      libraryDependencies ++= Seq(
        "org.scala-lang.modules.scalajs" %%% "scalajs-dom" % "0.6",
        "org.scala-lang.modules" %% "scala-async" % "0.9.2",
        "biz.cgta" %%% "otest-sjs" % "0.1.12",
        "biz.cgta" %%% "oscala-sjs" % "0.1.0",
        "biz.cgta" %%% "cenum-sjs" % "0.1.0",
        "biz.cgta" %%% "serland-sjs" % "0.1.0"
      )
    )
    .settings(
      includeFilter in (Assets, LessKeys.less) := "*.less",
      excludeFilter in (Assets, LessKeys.less) := "_*.less"
    )
    .settings(
      SbtIdeaPlugin.ideaBasePackage := Some("looty"),
      sourceGenerators in Assets <+= handleLootyHtml,
      libraryDependencies += "org.scala-lang.modules.scalajs" %%% "scalajs-jquery" % "0.6",
      libraryDependencies += "com.github.japgolly.scalajs-react" %%% "core" % "0.4.1")
    .settings((ScalaJSKeys.fastOptJS in Compile) <<= (ScalaJSKeys.fastOptJS in Compile).dependsOn(WebKeys.assets in Assets))
    .settings(sjsOutDir := WebKeys.webTarget.value / "public" / "main" )
    .settings(sjsTasks.map(t => crossTarget in(Compile, t) := sjsOutDir.value): _*)
    .enablePlugins(SbtWeb)

  lazy val root = Project("root", file(".")).aggregate(looty).enablePlugins(SbtWeb)

  object Libs {
  }

}

