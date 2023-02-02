package pl.msitko.realworld

import cats.effect.IO
import pl.msitko.realworld.services.{ArticleServices, ProfileServices, TagServices, UserServices}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Services:
  val apiServices: List[ServerEndpoint[Any, IO]] =
    ArticleServices.services ++ ProfileServices.services ++ TagServices.services ++ UserServices.services

  val docEndpoints: List[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
    .fromServerEndpoints[IO](apiServices, "real-world", "1.0.0")

  val prometheusMetrics: PrometheusMetrics[IO] = PrometheusMetrics.default[IO]()
  val metricsEndpoint: ServerEndpoint[Any, IO] = prometheusMetrics.metricsEndpoint

  val all: List[ServerEndpoint[Any, IO]] = apiServices ++ docEndpoints ++ List(metricsEndpoint)
