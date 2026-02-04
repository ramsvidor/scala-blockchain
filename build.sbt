ThisBuild / scalaVersion := "3.8.1"

val http4sVersion = "0.23.32"
val circeVersion = "0.14.15"

lazy val root = (project in file("."))
  .settings(
    name := "scala-blockchain",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.typelevel" %% "cats-effect" % "3.6.3",
      "org.bouncycastle" % "bcprov-jdk18on" % "1.83",
      "ch.qos.logback" % "logback-classic" % "1.5.27" % Runtime,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.7.0" % Test
    )
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
