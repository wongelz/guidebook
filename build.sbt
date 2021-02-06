import Path.rebase
import sbt.Keys.{organization, publishTo, _}
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

import scala.collection.immutable.Seq

lazy val commonSettings: Seq[Setting[_]] = Seq(
  organization := "com.github.wongelz",
  name := "guidebook",
  scalaVersion := "2.13.3"
)

lazy val client: Project = (project in file("client"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .settings(commonSettings)
  .settings(
    publishArtifact := false,
    publishLocal := {},
    publish := {},
    libraryDependencies ++= Seq(
      "be.doeraene" %%% "scalajs-jquery" % "1.0.0"
    ),
    npmDependencies in Compile += "jquery" -> "2.1.4",
    npmDependencies in Compile += "popper" -> "1.16.0",
    npmDependencies in Compile += "bootstrap" -> "4.4.1",
    scalaJSUseMainModuleInitializer := true,
    mainClass in Compile := Some("com.github.wongelz.guidebook.client.ClientMain")
  )

lazy val library: Project = (project in file("library"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest"           %% "scalatest"                % "3.2.2",
      "org.scalatestplus"       %% "selenium-3-141"           % "3.2.2.0",
      "org.seleniumhq.selenium" %  "selenium-java"            % "3.141.59",
      "com.lihaoyi"             %% "scalatags"                % "0.8.4",
      "io.circe"                %% "circe-core"               % "0.13.0",
      "io.circe"                %% "circe-generic"            % "0.13.0",
      "commons-codec"           %  "commons-codec"            % "1.13"
    ),
    compile in Compile := ((compile in Compile) dependsOn (fullOptJS in(client, Compile))).value,
    resourceGenerators in Compile += Def.task {
      val files = ((crossTarget in(client, Compile)).value ** ("*.js" || "*.map")).get
      val mappings: scala.Seq[(File, String)] = files pair rebase((crossTarget in(client, Compile)).value, ((resourceManaged in  Compile).value / "assets/").getAbsolutePath )
      val map: scala.Seq[(sbt.File, sbt.File)] = mappings.map { case (s, t) => (s, file(t))}
      IO.copy(map).toSeq
    }
  )

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(publishArtifact := false)
  .aggregate(library)

organization in ThisBuild := "com.github.wongelz"
homepage in ThisBuild := Some(url("https://github.com/wongelz/guidebook"))
scmInfo in ThisBuild := Some(ScmInfo(url("https://github.com/wongelz/guidebook"), "git@github.com:wongelz/guidebook.git"))
developers in ThisBuild := List(Developer("wongelz", "wongelz", "wongelz@gmail.com", url("https://github.com/wongelz")))
licenses in ThisBuild += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
publishMavenStyle in ThisBuild := true

publishTo in ThisBuild := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies, // check that there are no SNAPSHOT dependencies
  inquireVersions, // ask user to enter the current and next verion
  runClean, // clean
  runTest, // run tests
  setReleaseVersion, // set release version in version.sbt
  commitReleaseVersion, // commit the release version
  tagRelease, // create git tag
  releaseStepCommandAndRemaining("+publishSigned"), // run +publishSigned command to sonatype stage release
  setNextVersion, // set next version in version.sbt
  commitNextVersion, // commint next version
  releaseStepCommand("sonatypeRelease"), // run sonatypeRelease and publish to maven central
  pushChanges // push changes to git
)
