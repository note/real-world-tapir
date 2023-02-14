package pl.msitko.realworld

import cats.effect.{ExitCode, IO, IOApp}
import doobie.Transactor
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import pl.msitko.realworld.services.Services
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}

object Main extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =
    def routes(services: Services) =
      val serverOptions: Http4sServerOptions[IO] =
        Http4sServerOptions
          .customiseInterceptors[IO]
          .metricsInterceptor(Services.prometheusMetrics.metricsInterceptor())
          .options

      Http4sServerInterpreter[IO](serverOptions).toRoutes(services.all)

    for {
      appConfig <- AppConfig.loadConfig
      dbConfig = appConfig.db
      jdbcURL  = s"jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/${dbConfig.dbName}"
      _ <- DBMigration.migrate(jdbcURL, dbConfig.username, dbConfig.password)
      transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
        "org.postgresql.Driver",
        jdbcURL,
        dbConfig.username,
        dbConfig.password
      )
      services = Services(transactor, appConfig)
      exitCode <- BlazeServerBuilder[IO]
        .bindHttp(appConfig.server.port, appConfig.server.host)
        .withHttpApp(Router("/" -> routes(services)).orNotFound)
        .resource
        .use { _ =>
          for {
            _ <- IO.println(
              s"Go to http://${appConfig.server.host}:${appConfig.server.port}/docs to open SwaggerUI. Press ENTER key to exit.")
            _ <- IO.readLine
          } yield ()
        }
        .as(ExitCode.Success)
    } yield exitCode
