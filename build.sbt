import sbt.Credentials
import sbt.Keys.{credentials, organization, publishTo}
import scala.collection.immutable.Seq
import sbt.Keys._
import Path.rebase

lazy val commonSettings: Seq[Setting[_]] = Seq(
  organization := "com.github.wongelz",
  name := "guidebook",
  scalaVersion := "2.13.1"
)

lazy val client: Project = (project in file("client"))
  .settings(commonSettings)
  .settings(
    publishArtifact := false,
    publishLocal := {},
    publish := {},
    libraryDependencies ++= Seq(
      "be.doeraene" %%% "scalajs-jquery" % "0.9.6"
    ),
    skip in packageJSDependencies := false,
    jsDependencies ++= Seq(
      "org.webjars"     % "jquery"    % "2.1.4"       / "jquery.js" minified "jquery.min.js",
      "org.webjars.npm" % "popper.js" % "1.16.0"      / "dist/umd/popper.js" minified "dist/umd/popper.min.js",
      "org.webjars"     % "bootstrap" % "4.4.1"       / "js/bootstrap.js" minified "js/bootstrap.min.js" dependsOn "dist/umd/popper.js"
    ),
    scalaJSUseMainModuleInitializer := true,
    mainClass in Compile := Some("com.github.wongelz.guidebook.client.ClientMain")
  )
  .enablePlugins(ScalaJSPlugin)

lazy val library: Project = (project in file("library"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest"           %% "scalatest"                % "3.0.8",
      "org.seleniumhq.selenium" %  "selenium-java"            % "3.141.59",
      "com.lihaoyi"             %% "scalatags"                % "0.8.4",
      "io.circe"                %% "circe-core"               % "0.12.3",
      "io.circe"                %% "circe-generic"            % "0.12.3",
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

publishMavenStyle in ThisBuild := true
publishTo in ThisBuild := Some("bintray-wongelz-guidebook" at "https://api.bintray.com/maven/wongelz/guidebook/guidebook/")
credentials in ThisBuild += Credentials(Path.userHome / ".bintray" / ".credentials")
