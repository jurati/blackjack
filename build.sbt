name := "blackjack"

version := "0.1"

scalaVersion := "2.13.7"

val catsVersion = "2.2.0"
val http4sVersion = "0.21.22"
val catsEffectVersion = "2.2.0"
val log4CatsVersion = "1.1.1"
val circeVersion = "0.13.0"
val scalaTestVersion = "3.2.7.0"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-effect" % catsEffectVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-jdk-http-client" % "0.3.6",
  "io.chrisdavenport" %% "log4cats-slf4j" % log4CatsVersion,
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.scalatestplus" %% "scalacheck-1-15" % scalaTestVersion % Test,
  "org.scalatestplus" %% "selenium-3-141" % scalaTestVersion % Test,
  "com.codecommit" %% "cats-effect-testing-scalatest" % "0.4.1" % Test,
)
