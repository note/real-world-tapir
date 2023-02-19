package pl.msitko.realworld.services

import cats.effect.IO
import doobie.util.transactor.Transactor
import pl.msitko.realworld.AppConfig
import pl.msitko.realworld.db.{ArticleRepo, CommentRepo, FollowRepo, TagRepo, UserRepo}
import pl.msitko.realworld.endpoints.{ArticleEndpoints, ProfileEndpoints, UserEndpoints}
import pl.msitko.realworld.wiring.{ArticleWiring, ProfileWiring, TagWiring, UserWiring}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Services:
  val prometheusMetrics: PrometheusMetrics[IO] = PrometheusMetrics.default[IO]()
  val metricsEndpoint: ServerEndpoint[Any, IO] = prometheusMetrics.metricsEndpoint

  def apply(transactor: Transactor[IO], appConfig: AppConfig): List[ServerEndpoint[Any, IO]] =
    val repos = Repos.fromTransactor(transactor)

    val articleEndpoints     = new ArticleEndpoints(appConfig.jwt)
    val articleService       = ArticleService(repos)
    val articleEndpointsImpl = ArticleWiring.enpoints(articleEndpoints, articleService)

    val profileEndpoints     = new ProfileEndpoints(appConfig.jwt)
    val profileService       = ProfileService(repos)
    val profileEndpointsImpl = ProfileWiring.endpoints(profileEndpoints, profileService)

    val userEndpoints     = new UserEndpoints(appConfig.jwt)
    val userService       = UserService(repos, appConfig.jwt)
    val userEndpointsImpl = UserWiring.endpoints(userEndpoints, userService)

    val apiServices: List[ServerEndpoint[Any, IO]] =
      articleEndpointsImpl ++ profileEndpointsImpl ++ userEndpointsImpl ++ TagWiring.endpoints

    val docEndpoints: List[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
      .fromServerEndpoints[IO](apiServices, "real-world", "1.0.0")

    apiServices ++ docEndpoints ++ List(metricsEndpoint)

final case class Repos(
    articleRepo: ArticleRepo,
    commentRepo: CommentRepo,
    userRepo: UserRepo,
    followRepo: FollowRepo,
    tagRepo: TagRepo,
)

object Repos:
  def fromTransactor(transactor: Transactor[IO]) =
    Repos(
      articleRepo = new ArticleRepo(transactor),
      commentRepo = new CommentRepo(transactor),
      userRepo = new UserRepo(transactor),
      followRepo = new FollowRepo(transactor),
      tagRepo = new TagRepo(transactor)
    )
