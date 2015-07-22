organization := "com.pagerduty"

name := "mapper"

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % Test,
  "org.scalacheck" %% "scalacheck" % "1.12.3" % Test)
