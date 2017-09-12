import sbt.Credentials
import sbt.Keys.{credentials, organization, publishTo}
import scala.collection.immutable.Seq
import sbt.Keys._
import Path.rebase

lazy val commonSettings: Seq[Setting[_]] = Seq(
  organization := "com.github.wongelz",
  scalaVersion := "2.12.3"
)

lazy val client: Project = (project in file("client"))
  .settings(commonSettings)
  .settings(
    name := "client",
    publishArtifact := false,
    publishLocal := {},
    publish := {},
    libraryDependencies ++= Seq(
      "be.doeraene" %%% "scalajs-jquery" % "0.9.1"
    ),
    skip in packageJSDependencies := false,
    jsDependencies ++= Seq(
      "org.webjars"     % "jquery"    % "2.1.4"       / "jquery.js" minified "jquery.min.js",
      "org.webjars"     % "bootstrap" % "4.0.0-beta"  / "js/bootstrap.js" minified "js/bootstrap.min.js",
      "org.webjars.npm" % "popper.js" % "1.12.5"      / "dist/umd/popper.js" minified "dist/umd/popper.min.js"
    ),
    scalaJSUseMainModuleInitializer := true,
    mainClass in Compile := Some("com.github.wongelz.guidebook.client.ClientMain")
  )
  .enablePlugins(ScalaJSPlugin)

lazy val library: Project = (project in file("library"))
  .settings(commonSettings)
  .settings(
    name := "guidebook",
    libraryDependencies ++= Seq(
      "org.scalatest"           %% "scalatest"                % "3.0.1",
      "org.seleniumhq.selenium" % "selenium-java"             % "3.0.1",
      "com.lihaoyi"             %% "scalatags"                % "0.6.5",
      "io.circe"                %% "circe-core"               % "0.8.0",
      "io.circe"                %% "circe-generic"            % "0.8.0"
    ),
    compile in Compile := ((compile in Compile) dependsOn (fullOptJS in(client, Compile))).value,
    resourceGenerators in Compile += Def.task {
      val files = ((crossTarget in(client, Compile)).value ** ("*.js" || "*.map")).get
      val mappings: scala.Seq[(File, String)] = files pair rebase((crossTarget in(client, Compile)).value, ((resourceManaged in  Compile).value / "assets/").getAbsolutePath )
      val map: scala.Seq[(sbt.File, sbt.File)] = mappings.map { case (s, t) => (s, file(t))}
      IO.copy(map).toSeq
    },

    publishMavenStyle := true,
    publishTo := Some("bintray-wongelz-guidebook" at "https://api.bintray.com/maven/wongelz/guidebook/guidebook/"),
    credentials += Credentials(Path.userHome / ".bintray" / ".credentials")
  )

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(publishArtifact := false)
  .aggregate(library)