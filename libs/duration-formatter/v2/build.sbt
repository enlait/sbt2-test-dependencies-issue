ThisBuild / organization := "example"
ThisBuild / scalaVersion := "2.13.18"
ThisBuild / version      := "0.2.0"

lazy val root = (project in file(".")).settings(
  name := "duration-formatter"
)
