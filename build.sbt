lazy val commonSettings = Seq(
  organization := "elipatov",
  homepage := Some(new URL("https://github.com/elipatov/cheetah-db")),
  startYear := Some(2021),
  scalaVersion := "2.13.3",
  licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT"))),
  libraryDependencies += compilerPlugin("org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full))

val circeVersion = "0.13.0"
val http4sVersion = "0.21.22"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-Ymacro-annotations",
)

lazy val root = (project in file(".")
  settings (name := "cheetah-db")
  settings commonSettings
  settings (publish / skip  := true))

lazy val core = (project in file("core")
  settings (name := "cheetah-db-core")
  settings (version := "0.1.0-alfa")
  settings commonSettings
  settings (libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  "org.typelevel" %% "cats-core" % "2.3.1",
  "org.typelevel" %% "cats-effect" % "2.4.1",
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-optics" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.2.3" % Test,
  "org.scalaj" %% "scalaj-http" % "2.4.2" % Test,
  "org.scalacheck" %% "scalacheck" % "1.15.3" % Test,
  "org.scalatestplus" %% "scalacheck-1-15" % "3.3.0.0-SNAP3" % Test,
)))

