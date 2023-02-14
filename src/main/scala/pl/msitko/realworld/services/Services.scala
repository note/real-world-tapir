package pl.msitko.realworld.services

import cats.effect.IO
import doobie.util.transactor.Transactor
import pl.msitko.realworld.AppConfig
import pl.msitko.realworld.db.{ArticleRepo, CommentRepo, FollowRepo, UserRepo}
import pl.msitko.realworld.endpoints.{ArticleEndpoints, ProfileEndpoints, UserEndpoints}
import pl.msitko.realworld.wiring.{ArticleWiring, ProfileWiring, TagWiring, UserWiring}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Services:
  val prometheusMetrics: PrometheusMetrics[IO] = PrometheusMetrics.default[IO]()
  val metricsEndpoint: ServerEndpoint[Any, IO] = prometheusMetrics.metricsEndpoint

  def apply(transactor: Transactor[IO], appConfig: AppConfig): List[ServerEndpoint[Any, IO]] =
    val articleRepo = new ArticleRepo(transactor)
    val commentRepo = new CommentRepo(transactor)
    val userRepo    = new UserRepo(transactor)
    val followRepo  = new FollowRepo(transactor)

    val articleEndpoints     = new ArticleEndpoints(appConfig.jwt)
    val articleService       = new ArticleService(articleRepo, commentRepo, followRepo)
    val articleEndpointsImpl = ArticleWiring.enpoints(articleEndpoints, articleService)

    val profileEndpoints     = new ProfileEndpoints(appConfig.jwt)
    val profileService       = new ProfileService(followRepo, userRepo)
    val profileEndpointsImpl = ProfileWiring.endpoints(profileEndpoints, profileService)

    val userEndpoints     = new UserEndpoints(appConfig.jwt)
    val userService       = new UserService(userRepo, appConfig.jwt)
    val userEndpointsImpl = UserWiring.endpoints(userEndpoints, userService)

    val apiServices: List[ServerEndpoint[Any, IO]] =
      articleEndpointsImpl ++ profileEndpointsImpl ++ userEndpointsImpl ++ TagWiring.endpoints

    val docEndpoints: List[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
      .fromServerEndpoints[IO](apiServices, "real-world", "1.0.0")

    apiServices ++ docEndpoints ++ List(metricsEndpoint)
