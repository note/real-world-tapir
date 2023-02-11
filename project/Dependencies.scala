import sbt._

object Dependencies {
	val tapirVersion = "1.2.6"

	val tapir = Seq(
		"ch.qos.logback"              % "logback-classic"           % "1.4.5",
		"com.softwaremill.sttp.tapir" %% "tapir-http4s-server"      % tapirVersion,
		"com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % tapirVersion,
		"com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % tapirVersion,
		"com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle"  % tapirVersion,
		"org.http4s"                  %% "http4s-blaze-server"      % "0.23.13",
  	)
	val compileDeps = tapir ++ Seq(
		"com.github.jwt-scala" 				%% "jwt-circe" 										% "9.1.2",
		"com.github.pureconfig" 			%% "pureconfig-core" 							% "0.17.2",
		"org.flywaydb" 								%  "flyway-core" 									% "9.14.1",
		"org.tpolecat" 								%% "doobie-core" 									% "1.0.0-RC2",
		"org.tpolecat" 								%% "doobie-hikari" 								% "1.0.0-RC2",
		"org.tpolecat" 								%% "doobie-postgres" 							% "1.0.0-RC2",
	)

	val testDeps    = Seq(
		"org.scalameta" %% "munit" % "0.7.29" % Test
	)
}
