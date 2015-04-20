name := """Thorvarium"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  ws,
  "nl.rhinofly" %% "play-s3" % "5.0.2",
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.4",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.4" % "test",
  "org.xerial" % "sqlite-jdbc" % "3.7.2",
  "org.scalatestplus" %% "play" % "1.1.0" % "test",
  "net.debasishg" %% "redisclient" % "2.13"
)

resolvers += "Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local"
