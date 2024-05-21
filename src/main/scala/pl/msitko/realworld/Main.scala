package pl.msitko.realworld

import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.typesafe.scalalogging.StrictLogging
import doobie.Transactor
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{Router, Server}
import pl.msitko.realworld.services.Services
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}

object Main extends IOApp with StrictLogging:

  private def routez(services: List[ServerEndpoint[Any, IO]]) =
    val serverOptions: Http4sServerOptions[IO] =
      Http4sServerOptions
        .customiseInterceptors[IO]
        .metricsInterceptor(Services.prometheusMetrics.metricsInterceptor())
        .corsInterceptor(sttp.tapir.server.interceptor.cors.CORSInterceptor.default)
        .options

    Http4sServerInterpreter[IO](serverOptions).toRoutes(services)

  private def getServer: IO[Resource[IO, Server]] =
    for {
      appConfig <- AppConfig.loadConfig
      dbConfig = appConfig.db
      _ <- IO(logger.info(s"Using config: $appConfig"))
      jdbcURL = s"jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/${dbConfig.dbName}"
      _ <- IO(logger.info(s"Using jdbcURL: $jdbcURL"))
      _ <- DBMigration.migrate(jdbcURL, dbConfig.username, dbConfig.password)
      transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
        driver = "org.postgresql.Driver",
        url = jdbcURL,
        user = dbConfig.username,
        password = dbConfig.password,
        logHandler = None
      )
      services = Services(transactor, appConfig)
    } yield EmberServerBuilder
      .default[IO]
      .withHost(appConfig.server.host)
      .withPort(appConfig.server.port)
      .withHttpApp(Router("/" -> routez(services)).orNotFound)
      .build

  def run(args: List[String]): IO[ExitCode] =
    for {
      server <- getServer
      _      <- server.useForever
    } yield ExitCode.Error
