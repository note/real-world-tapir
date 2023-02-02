package pl.msitko.realworld

import cats.effect.{ExitCode, IO, IOApp}
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import pureconfig.ConfigSource
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}

object DBMigration {
  def migrate(url: String, user: String, password: String): IO[MigrateResult] =
    IO {
      Flyway
        .configure()
        .dataSource(url, user, password)
        .load()
        .migrate()
    }
}

object Main extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =
    val routes =
      val serverOptions: Http4sServerOptions[IO] =
        Http4sServerOptions
          .customiseInterceptors[IO]
          .metricsInterceptor(Services.prometheusMetrics.metricsInterceptor())
          .options
      Http4sServerInterpreter[IO](serverOptions).toRoutes(Services.all)

    for {
      appConfig <- AppConfig.loadConfig
      dbConfig = appConfig.db
      _ <- DBMigration.migrate(
        s"jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/${dbConfig.dbName}",
        dbConfig.username,
        dbConfig.password)
      exitCode <- BlazeServerBuilder[IO]
        .bindHttp(appConfig.server.port, appConfig.server.host)
        .withHttpApp(Router("/" -> routes).orNotFound)
        .resource
        .use { server =>
          for {
            _ <- IO.println(
              s"Go to http://${appConfig.server.host}:${appConfig.server.port}/docs to open SwaggerUI. Press ENTER key to exit.")
            _ <- IO.readLine
          } yield ()
        }
        .as(ExitCode.Success)
    } yield exitCode
