package pl.msitko.realworld

import cats.effect.{ExitCode, IO, IOApp, Resource}
import doobie.Transactor
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.{Router, Server}
import pl.msitko.realworld.services.Services
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}

object Main extends IOApp:

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
      jdbcURL  = s"jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/${dbConfig.dbName}"
//      _ <- DBMigration.migrate(jdbcURL, dbConfig.username, dbConfig.password)
      transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
        "org.postgresql.Driver",
        jdbcURL,
        dbConfig.username,
        dbConfig.password
      )
      services = Services(transactor, appConfig)
    } yield BlazeServerBuilder[IO]
      .bindHttp(appConfig.server.port, appConfig.server.host)
      .withHttpApp(Router("/" -> routez(services)).orNotFound)
      .resource

  override def run(args: List[String]): IO[ExitCode] =
    for {
      server <- getServer
      _      <- server.useForever
    } yield ExitCode.Error
