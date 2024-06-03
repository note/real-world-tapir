import Common._

lazy val model = (project in file("model"))
  .commonSettings("model")
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % Dependencies.tapirVersion,
    )
  )

lazy val client = (project in file("client"))
  .commonSettings("client")
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-client"        % Dependencies.tapirVersion,
    )
  )
  .dependsOn(model)

lazy val server = (project in file("server"))
  .dependsOn(model)
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
    dockerBaseImage := "eclipse-temurin:21-jre",
    dockerUpdateLatest := true, // docker:publishLocal will replace the latest tagged image.
    dockerExposedPorts ++= Seq(8080),
    Docker / defaultLinuxInstallLocation := "/opt/realworld",
    dockerBuildCommand := {
      // use buildx with platform to build supported amd64 images on other CPU architectures
      // this may require that you have first run 'docker buildx create' to set docker buildx up
      dockerExecCommand.value ++ Seq("buildx", "build", "--platform=linux/amd64", "--load") ++ dockerBuildOptions.value :+ "."
    },
    Universal / javaOptions ++= Seq(
      "-J-XX:+PrintFlagsFinal"
    )
//    Revolver.enableDebugging(port = 5050, suspend = true)
  )
