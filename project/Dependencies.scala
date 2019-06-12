import sbt._

object Dependencies {

  lazy val catsEffectV = "1.3.0"
  lazy val scoptV = "4.0.0-RC2"
  lazy val circeV = "0.10.0"
  lazy val scalaTestV = "3.0.5"

  lazy val castEffect = "org.typelevel" %% "cats-effect" % catsEffectV
  lazy val scopt = "com.github.scopt" %% "scopt" % scoptV
  lazy val circe = "io.circe" %% "circe-core" % circeV
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeV
  lazy val circeGenericExtras = "io.circe" %% "circe-generic-extras" % circeV
  lazy val circeParser = "io.circe" %% "circe-parser" % circeV

  lazy val scalaTest = "org.scalatest" %% "scalatest" % scalaTestV % "test"

  lazy val dependencies: Seq[ModuleID] = Seq(
    castEffect,
    scopt,
    circe,
    circeGeneric,
    circeGenericExtras,
    circeParser,
    scalaTest
  )

}
