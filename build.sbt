name := "damino-bot-webhook"

version := "0.1"

scalaVersion := "2.12.12"

enablePlugins(JavaAppPackaging)

val akkaVersion = "2.6.7"
val jsonVersion = "3.6.9"
val xtractVersion = "2.0.0"

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-native" % jsonVersion,
  "org.json4s" %% "json4s-jackson" % jsonVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.1.12",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "de.heikoseeberger" %% "akka-http-json4s" % "1.31.0",
  "com.lucidchart" %% "xtract" % xtractVersion,
  "com.lucidchart" %% "xtract-testing" % xtractVersion % "test",
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0"
)