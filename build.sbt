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
      "org.scala-js" %%% "scalajs-dom" % "2.2.0"
    ),
    Compile / npmDependencies ++= Seq(
      "bootstrap" -> "5.1.3"
    ),
    scalaJSUseMainModuleInitializer := true,
    Compile / mainClass := Some("com.github.wongelz.guidebook.client.ClientMain")
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
    Compile / compile := ((compile in Compile) dependsOn (fullOptJS / webpack in(client, Compile))).value,
    Compile / resourceGenerators += Def.task {
      val files = ((crossTarget in(client, Compile)).value ** ("*guidebook*.js" || "*guidebook*.map")).get
      val mappings: scala.Seq[(File, String)] = files pair rebase((crossTarget in(client, Compile)).value, ((resourceManaged in  Compile).value / "assets/").getAbsolutePath )
      val map: scala.Seq[(sbt.File, sbt.File)] = mappings.map { case (s, t) => (s, file(t))}
      IO.copy(map).toSeq
    }
  )

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(publishArtifact := false)
  .aggregate(library)

ThisBuild / organization := "com.github.wongelz"
ThisBuild / homepage := Some(url("https://github.com/wongelz/guidebook"))
ThisBuild / scmInfo := Some(ScmInfo(url("https://github.com/wongelz/guidebook"), "git@github.com:wongelz/guidebook.git"))
ThisBuild / developers := List(Developer("wongelz", "wongelz", "wongelz@gmail.com", url("https://github.com/wongelz")))
ThisBuild / licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / publishMavenStyle := true

ThisBuild / publishTo := Some(
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
