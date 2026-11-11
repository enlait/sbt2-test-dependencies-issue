ThisBuild / organization := "example"
ThisBuild / scalaVersion := "2.13.18"
ThisBuild / version      := "0.1.0"

lazy val root = (project in file(".")).settings(
  name := "duration-report",
  libraryDependencies += "example" %% "duration-formatter" % "0.1.0"
)

