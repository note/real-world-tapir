import sbt._

object Dependencies {
  val tapirVersion  = "1.8.2"
  val doobieVersion = "1.0.0-RC5"
  val flywayVersion = "10.11.0"

  val tapir = Seq(
    "ch.qos.logback"               % "logback-classic"          % "1.4.11",
    "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"      % tapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % tapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % tapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle"  % tapirVersion,
    "org.http4s"                  %% "http4s-blaze-server"      % "0.23.16",
  )
  val compileDeps = tapir ++ Seq(
    "com.github.jwt-scala"       %% "jwt-circe"                   % "10.0.0",
    "com.github.pureconfig"      %% "pureconfig-core"             % "0.17.6",
    "com.typesafe.scala-logging" %% "scala-logging"               % "3.9.5",
    "org.flywaydb"                % "flyway-core"                 % flywayVersion,
    "org.flywaydb"                % "flyway-database-postgresql"  % flywayVersion,
    "org.tpolecat"               %% "doobie-core"                 % doobieVersion,
    "org.tpolecat"               %% "doobie-hikari"               % doobieVersion,
    "org.tpolecat"               %% "doobie-postgres"             % doobieVersion,
  )

  val testDeps = Seq(
    "com.dimafeng"  %% "testcontainers-scala-postgresql" % "0.41.0"    % Test,
    "com.dimafeng"  %% "testcontainers-scala-munit"      % "0.41.0"    % Test,
    "org.scalameta" %% "munit"                           % "1.0.0-M10" % Test,
    "org.typelevel" %% "munit-cats-effect"               % "2.0.0-M3"  % Test,
  )
}
