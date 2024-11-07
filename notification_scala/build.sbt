ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

lazy val root = (project in file("."))
  .settings(
    name := "notification_scala"
  )

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-native" % "3.6.7",
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "org.postgresql" % "postgresql" % "42.2.20"
)
