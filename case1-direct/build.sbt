ThisBuild / scalaVersion := "2.13.18"

// Failure mode 1: the library is a direct dependency of the app.
lazy val root = (project in file(".")).settings(
  name := "direct",
  libraryDependencies ++= Seq(
    // bump to 0.2.0 to see this failure mode
    "example" %% "duration-formatter" % "0.1.0",
    "org.scalameta" %% "munit" % "1.0.0" % Test
  )
)
