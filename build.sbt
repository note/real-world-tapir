import Common._

lazy val root = (project in file("."))
  .commonSettings("real-world-tapir", "0.1.0")
  .settings(
    libraryDependencies ++= Dependencies.compileDeps ++ Dependencies.testDeps
  )

assembly / assemblyMergeStrategy := {
  case d if d.endsWith(".jar:module-info.class") => MergeStrategy.first
  case d if d.endsWith("module-info.class") => MergeStrategy.first
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
