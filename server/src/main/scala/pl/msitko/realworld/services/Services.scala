package pl.msitko.realworld.services

import cats.effect.IO
import doobie.util.transactor.Transactor
import pl.msitko.realworld.AppConfig
import pl.msitko.realworld.db.{ArticleRepo, CommentRepo, FollowRepo, HealthRepo, TagRepo, UserRepo}
import pl.msitko.realworld.endpoints.HealthEndpoint
import pl.msitko.realworld.wiring.{ArticleWiring, AuthLogic, ProfileWiring, TagWiring, UserWiring}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Services:
  val prometheusMetrics: PrometheusMetrics[IO] = PrometheusMetrics.default[IO]()
  val metricsEndpoint: ServerEndpoint[Any, IO] = prometheusMetrics.metricsEndpoint

  def apply(transactor: Transactor[IO], appConfig: AppConfig): List[ServerEndpoint[Any, IO]] =
    val repos = Repos.fromTransactor(transactor)

    val authLogic = AuthLogic(appConfig.jwt)

    val articleService       = ArticleService(repos)
    val articleEndpointsImpl = ArticleWiring(authLogic).endpoints(articleService)

    val profileService       = ProfileService(repos)
    val profileEndpointsImpl = ProfileWiring(authLogic).endpoints(profileService)

    val userService       = UserService(repos, appConfig.jwt)
    val userEndpointsImpl = UserWiring(authLogic).endpoints(userService)

    val tagService       = TagService(repos)
    val tagEndpointsImpl = TagWiring.endpoints(tagService)

    val healthService      = HealthService(repos)
    val healthEndpointImpl = HealthEndpoint.health.serverLogicSuccess(_ => healthService.getHealth)

    val apiServices: List[ServerEndpoint[Any, IO]] =
      userEndpointsImpl ++ articleEndpointsImpl ++ profileEndpointsImpl ++ tagEndpointsImpl ++ List(healthEndpointImpl)

    val docEndpoints: List[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
      .fromServerEndpoints[IO](apiServices, "real-world", "1.0.0")

    apiServices ++ List(metricsEndpoint) ++ docEndpoints

final case class Repos(
    articleRepo: ArticleRepo,
    commentRepo: CommentRepo,
    userRepo: UserRepo,
    followRepo: FollowRepo,
    tagRepo: TagRepo,
    healthRepo: HealthRepo,
)

object Repos:
  def fromTransactor(transactor: Transactor[IO]) =
    Repos(
      articleRepo = new ArticleRepo(transactor),
      commentRepo = new CommentRepo(transactor),
      userRepo = new UserRepo(transactor),
      followRepo = new FollowRepo(transactor),
      tagRepo = new TagRepo(transactor),
      healthRepo = new HealthRepo(transactor),
    )
