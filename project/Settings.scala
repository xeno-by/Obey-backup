import sbt.Keys._
import sbt._
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin._

object Settings {
  lazy val languageVersion = "2.11.6"
  lazy val metaVersion = "0.1.0-SNAPSHOT"

  lazy val sharedSettings: Seq[sbt.Def.Setting[_]] = Defaults.defaultSettings ++ Seq(
    scalaVersion := languageVersion,
    crossVersion := CrossVersion.full,
    version := metaVersion,
    organization := "org.obey",
    description := "Code Health compiler plugin for scalameta trees",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked"),
    parallelExecution in Test := false, // hello, reflection sync!!
    logBuffered := false) ++ PublishSettings.publishSettings

  /* Forcing version to be the same for the SBT plugin and for model and plugin. */
  lazy val sbtPluginSettings: Seq[sbt.Def.Setting[_]] = Defaults.defaultSettings ++ Seq(
    version := metaVersion
  ) ++ PublishSettings.publishSettings

  lazy val flatLayout: Seq[sbt.Def.Setting[_]] = assemblySettings ++ Seq(
    scalaSource in Compile <<= (baseDirectory in Compile)(base => base),
    resourceDirectory in Compile <<= (baseDirectory in Compile)(base => base / "resources"))

  lazy val mergeDependencies: Seq[sbt.Def.Setting[_]] = assemblySettings ++ Seq(
    test in assembly := {},
    mergeStrategy in assembly := {
      case "scalac-plugin.xml" => MergeStrategy.first
      case x =>
        val oldStrategy = (mergeStrategy in assembly).value
        oldStrategy(x)
      //case _ => MergeStrategy.filterDistinctLines
    },
    logLevel in assembly := Level.Error,
    jarName in assembly := "obey_"+name.value + "_" + scalaVersion.value + "-" + version.value + "-assembly.jar",
    assemblyOption in assembly ~= { _.copy(includeScala = false) },
    Keys.`package` in Compile := {
      val slimJar = (Keys.`package` in Compile).value
      val fatJar = new File(crossTarget.value + "/" + (jarName in assembly).value)
      val _ = assembly.value
      IO.copy(List(fatJar -> slimJar), overwrite = true)
      slimJar
    },
    packagedArtifact in Compile in packageBin := {
      val temp = (packagedArtifact in Compile in packageBin).value
      val (art, slimJar) = temp
      val fatJar = new File(crossTarget.value + "/" + (jarName in assembly).value)
      val _ = assembly.value
      IO.copy(List(fatJar -> slimJar), overwrite = true)
      (art, slimJar)
    })

  lazy val dontPackage = packagedArtifacts := Map.empty

  // Thanks Jason for this cool idea (taken from https://github.com/retronym/boxer)
  // add plugin timestamp to compiler options to trigger recompile of
  // main after editing the plugin. (Otherwise a 'clean' is needed.)
  def usePlugin(plugin: ProjectReference) =
    scalacOptions <++= (Keys.`package` in (plugin, Compile)) map { (jar: File) =>
      System.setProperty("sbt.paths.plugin.jar", jar.getAbsolutePath)
      Seq("-Xplugin:" + jar.getAbsolutePath, "-Jdummy=" + jar.lastModified)
    }

  def exposeClasspaths(projectName: String) = Seq(
    fullClasspath in Test := {
      val defaultValue = (fullClasspath in Test).value
      val classpath = defaultValue.files.map(_.getAbsolutePath)
      System.setProperty("sbt.paths.tests.classpath", classpath.mkString(java.io.File.pathSeparatorChar.toString))
      defaultValue
    },
    resourceDirectory in Test := {
      val defaultValue = (resourceDirectory in Test).value
      System.setProperty("sbt.paths.tests.resources", defaultValue.getAbsolutePath)
      defaultValue
    })
}
