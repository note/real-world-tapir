import Common._

lazy val root = (project in file("."))
  .commonSettings("real-world-tapir", "0.1.0")
  .settings(
    libraryDependencies ++= Dependencies.compileDeps ++ Dependencies.testDeps
  )
