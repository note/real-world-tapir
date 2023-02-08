package pl.msitko.realworld.services

import cats.effect.IO
import doobie.util.transactor.Transactor
import pl.msitko.realworld.services.{ArticleServices, ProfileServices, TagServices, UserServices}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter

class Services(userServices: UserServices):
  import Services.*

  val apiServices: List[ServerEndpoint[Any, IO]] =
    ArticleServices.services ++ ProfileServices.services ++ TagServices.services ++ userServices.services

  val docEndpoints: List[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
    .fromServerEndpoints[IO](apiServices, "real-world", "1.0.0")

  val all: List[ServerEndpoint[Any, IO]] = apiServices ++ docEndpoints ++ List(metricsEndpoint)

object Services:
  val prometheusMetrics: PrometheusMetrics[IO] = PrometheusMetrics.default[IO]()
  val metricsEndpoint: ServerEndpoint[Any, IO] = prometheusMetrics.metricsEndpoint

  def apply(transactor: Transactor[IO]): Services =
    val userServices = new UserServices(transactor)
    new Services(userServices)
