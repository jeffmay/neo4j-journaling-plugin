name := "neo4j-plugins"

organization := "me.jeffmay"

version := "0.0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.4.3",
  // "com.typesafe.play" %% "play-ws" % "2.4.3",
  "org.mongodb.scala" %% "mongo-scala-driver" % "1.0.0",
  "org.neo4j.app" % "neo4j-server" % "2.3.0",
  "org.neo4j" % "server-api" % "2.3.0",
  "javax.ws.rs" % "javax.ws.rs-api" % "2.0"
)

