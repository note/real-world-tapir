import Common._

enablePlugins(GitVersioning)

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .commonSettings("real-world-tapir")
  .settings(
    libraryDependencies ++= Dependencies.compileDeps ++ Dependencies.testDeps,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "pl.msitko.realworld.buildinfo"
  )
  .enablePlugins(
    JavaAppPackaging,
    DockerPlugin // Enable the docker plugin
  )
  .settings(
    Docker / packageName := "msitko.pl/real-world-tapir",
    packageDescription := "real-world-tapir",
    dockerBaseImage := "eclipse-temurin:17",
    dockerUpdateLatest := true, // docker:publishLocal will replace the latest tagged image.
    dockerExposedPorts ++= Seq(8080),
    Docker / defaultLinuxInstallLocation := "/opt/realworld"
  )

assembly / assemblyMergeStrategy := {
  case d if d.endsWith(".jar:module-info.class") => MergeStrategy.first
  case d if d.endsWith("module-info.class") => MergeStrategy.first
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
