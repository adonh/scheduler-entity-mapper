organization := "com.pagerduty"

name := "entity-mapper"

scalaVersion := "2.11.11"

crossScalaVersions := Seq("2.10.6", "2.11.11", "2.12.2")

libraryDependencies ++= Seq("org.slf4j" % "slf4j-api" % "1.7.12")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % Test
)

scalafmtOnCompile in ThisBuild := true
