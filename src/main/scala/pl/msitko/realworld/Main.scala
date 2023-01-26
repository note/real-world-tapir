package pl.msitko.realworld

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}

object Main extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =

    val serverOptions: Http4sServerOptions[IO] =
      Http4sServerOptions
        .customiseInterceptors[IO]
        .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
        .options
    val routes = Http4sServerInterpreter[IO](serverOptions).toRoutes(Endpoints.all)

    val port = sys.env.get("HTTP_PORT").flatMap(_.toIntOption).getOrElse(8080)

    BlazeServerBuilder[IO]
      .bindHttp(port, "localhost")
      .withHttpApp(Router("/" -> routes).orNotFound)
      .resource
      .use { server =>
        for {
          _ <- IO.println(
            s"Go to http://localhost:${server.address.getPort}/docs to open SwaggerUI. Press ENTER key to exit.")
          _ <- IO.readLine
        } yield ()
      }
      .as(ExitCode.Success)
