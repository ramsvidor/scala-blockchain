ThisBuild / scalaVersion := "3.3.3"

val http4sVersion = "0.23.28"
val circeVersion = "0.14.7"

lazy val root = (project in file("."))
  .settings(
    name := "scala-blockchain",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "org.bouncycastle" % "bcprov-jdk18on" % "1.78.1",
      "ch.qos.logback" % "logback-classic" % "1.5.6" % Runtime,
      "org.scalatest" %% "scalatest" % "3.2.18" % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test
    )
  )

Global / onChangedBuildSource := ReloadOnSourceChanges

