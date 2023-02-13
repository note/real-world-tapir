package pl.msitko.realworld.services

import cats.effect.IO
import doobie.util.transactor.Transactor
import pl.msitko.realworld.AppConfig
import pl.msitko.realworld.db.{ArticleRepo, CommentRepo, FollowRepo, UserRepo}
import pl.msitko.realworld.services.{ArticleServices, ProfileServices, TagServices, UserServices}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter

class Services(articleServices: ArticleServices, userServices: UserServices, profileServices: ProfileServices):
  import Services.*

  val apiServices: List[ServerEndpoint[Any, IO]] =
    articleServices.services ++ profileServices.services ++ TagServices.services ++ userServices.services

  val docEndpoints: List[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
    .fromServerEndpoints[IO](apiServices, "real-world", "1.0.0")

  val all: List[ServerEndpoint[Any, IO]] = apiServices ++ docEndpoints ++ List(metricsEndpoint)

object Services:
  val prometheusMetrics: PrometheusMetrics[IO] = PrometheusMetrics.default[IO]()
  val metricsEndpoint: ServerEndpoint[Any, IO] = prometheusMetrics.metricsEndpoint

  def apply(transactor: Transactor[IO], appConfig: AppConfig): Services =
    val articleRepo = new ArticleRepo(transactor)
    val commentRepo = new CommentRepo(transactor)
    val userRepo    = new UserRepo(transactor)
    val followRepo  = new FollowRepo(transactor)

    val articleServices = new ArticleServices(articleRepo, commentRepo, appConfig.jwt)
    val userServices    = new UserServices(userRepo, appConfig.jwt)
    val profileServices = new ProfileServices(followRepo, userRepo, appConfig.jwt)

    new Services(articleServices, userServices, profileServices)
