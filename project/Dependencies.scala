import sbt._

object Dependencies {
	val tapirVersion = "1.2.6"

	val tapir = Seq(
		"com.softwaremill.sttp.tapir" %% "tapir-http4s-server"      % tapirVersion,
		"org.http4s"                  %% "http4s-blaze-server"      % "0.23.13",
		"com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % tapirVersion,
		"com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle"  % tapirVersion,
		"com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % tapirVersion,
		"ch.qos.logback"              % "logback-classic"           % "1.4.5",
  	)
	val compileDeps = tapir

	val testDeps    = Seq(
		"org.scalameta" %% "munit" % "0.7.29" % Test
	)
}
