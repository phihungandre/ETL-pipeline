ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

lazy val root = (project in file("."))
  .settings(
    name := "consumer_alert"
  )

libraryDependencies += "org.postgresql" % "postgresql" % "42.2.20"
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "2.8.1"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.3.0"
libraryDependencies += "org.apache.spark" %% "spark-streaming" % "3.3.0"
libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.3.0"
libraryDependencies += "org.apache.hadoop" % "hadoop-client" % "3.3.2"