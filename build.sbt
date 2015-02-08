name := """Thorvarium"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  ws, // Play's web services module
  "net.debasishg" %% "redisreact" % "0.7",
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.4",
  "org.webjars" % "bootstrap" % "3.2.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.4" % "test",
  "org.xerial" % "sqlite-jdbc" % "3.7.2"
)
