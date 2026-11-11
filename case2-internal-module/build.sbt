ThisBuild / scalaVersion := "2.13.18"

// Failure mode 2: the library is consumed by an internal module; the tests live in the
// depending module.
lazy val core = project.settings(
  name := "core",
  // bump to 0.2.0 to see this failure mode
  libraryDependencies += "example" %% "duration-formatter" % "0.1.0"
)

lazy val root = (project in file(".")).settings(
  name := "internal-module",
  libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test
).dependsOn(core)
