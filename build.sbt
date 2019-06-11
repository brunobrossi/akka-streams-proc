lazy val root = (project in file(".")).
    settings(
      name := "akka-streams-proc",
      version := "1.0",
      scalaVersion := "2.12.8",
      mainClass in Compile := Some("com.simplaex.MessageConsumer")
    )

val akkaVersion = "2.5.22"
val scalaTestVersion = "3.0.4"
libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.4",
  //akka
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.lightbend.akka" %% "akka-stream-alpakka-udp" % "1.0.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "org.scalactic" %% "scalactic" % scalaTestVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test
)
