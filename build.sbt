name := """Thorvarium"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

javaOptions in Test ++= Option(System.getProperty("config.file")).map("-Dconfig.file=" + _).toSeq

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  ws, // Play's web services module
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.4",
  "org.webjars" % "bootstrap" % "3.2.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.4" % "test",
  "org.xerial" % "sqlite-jdbc" % "3.7.2",
  "org.scalatestplus" %% "play" % "1.1.0" % "test",
  "net.debasishg" %% "redisclient" % "2.13"
)
