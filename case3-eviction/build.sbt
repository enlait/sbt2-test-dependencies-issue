ThisBuild / scalaVersion := "2.13.18"

// Failure mode 3: the version change enters the classpath purely through resolution. A
// security-fix-style pin (referenced by no app code) evicts the transitive duration-formatter
// 0.1.0, changing behavior through duration-report's published bytecode.
lazy val root = (project in file(".")).settings(
  name := "eviction",
  libraryDependencies ++= Seq(
    "example" %% "duration-report" % "0.1.0",
    "org.scalameta" %% "munit" % "1.0.0" % Test
  )
  // Uncomment to see this failure mode: the pin evicts duration-formatter 0.1.0 from the
  // classpath, and no app code references it:
  // , dependencyOverrides += "example" %% "duration-formatter" % "0.2.0"
)
