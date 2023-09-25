import sbt._

object Dependencies {
	val tapirVersion = "1.7.4"
	val doobieVersion = "1.0.0-RC2"

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
		"com.typesafe.scala-logging" 	%% "scala-logging" 								% "3.9.4",
		"org.flywaydb" 								%  "flyway-core" 									% "9.15.1",
		"org.tpolecat" 								%% "doobie-core" 									% doobieVersion,
		"org.tpolecat" 								%% "doobie-hikari" 								% doobieVersion,
		"org.tpolecat" 								%% "doobie-postgres" 							% doobieVersion,
	)

	val testDeps    = Seq(
		"com.dimafeng"  %% "testcontainers-scala-postgresql" % "0.40.12"  % Test,
		"com.dimafeng"  %% "testcontainers-scala-munit"      % "0.40.12"  % Test,
		"org.scalameta" %% "munit" 													 % "0.7.29" 	% Test,
		"org.typelevel" %% "munit-cats-effect-3" 						 % "1.0.7" 		% Test,
	)
}
