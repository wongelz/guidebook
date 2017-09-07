name := "guidebook"

organization := "com.github.wongelz"

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  "org.scalatest"           %% "scalatest"                % "3.0.1",
  "org.seleniumhq.selenium" %  "selenium-java"            % "3.0.1",
  "com.lihaoyi"             %% "scalatags"                % "0.6.5",
  "io.circe"                %% "circe-core"               % "0.8.0",
  "io.circe"                %% "circe-generic"            % "0.8.0"
)

publishMavenStyle := true

publishTo := Some("bintray-wongelz-guidebook" at "https://api.bintray.com/maven/wongelz/guidebook/guidebook/")

credentials += Credentials(Path.userHome / ".bintray" / ".credentials")