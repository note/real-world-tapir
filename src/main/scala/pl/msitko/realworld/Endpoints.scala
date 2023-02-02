package pl.msitko.realworld

import sttp.tapir.*

import Library.*
import cats.effect.IO
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object ExampleResponses:
  val user: Endpoints.User = Endpoints.User(
    email = "a@example.com",
    token = "abc",
    username = "user",
    bio = "bio",
    image = None
  )
  val userBody: Endpoints.UserBody = user.body

object Endpoints:

  final case class AuthenticationReqBodyUser(email: String, password: String)
  final case class AuthenticationReqBody(user: AuthenticationReqBodyUser)

  final case class User(
      email: String,
      token: String,
      username: String,
      bio: String,
      image: Option[String],
  ):
    def body: UserBody = UserBody(user = this)

  final case class UserBody(user: User)
  val authentication = endpoint.post
    .in("api" / "users" / "login")
    .in(jsonBody[AuthenticationReqBody])
    .out(jsonBody[UserBody])
  val authenticationImpl: ServerEndpoint[Any, IO] =
    authentication.serverLogicSuccess(_ => IO.pure(ExampleResponses.userBody))

  final case class RegistrationUserBody(username: String, email: String, password: String)
  final case class RegistrationReqBody(user: RegistrationUserBody)

  val registration = endpoint.post
    .in("api" / "users")
    .in(jsonBody[RegistrationReqBody])
    .out(jsonBody[UserBody])
  val registrationImpl: ServerEndpoint[Any, IO] =
    registration.serverLogicSuccess(_ => IO.pure(ExampleResponses.userBody))

  val apiEndpoints: List[ServerEndpoint[Any, IO]] = List(authenticationImpl, registrationImpl)

  val docEndpoints: List[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
    .fromServerEndpoints[IO](apiEndpoints, "stiff-halibut", "1.0.0")

  val prometheusMetrics: PrometheusMetrics[IO] = PrometheusMetrics.default[IO]()
  val metricsEndpoint: ServerEndpoint[Any, IO] = prometheusMetrics.metricsEndpoint

  val all: List[ServerEndpoint[Any, IO]] = apiEndpoints ++ docEndpoints ++ List(metricsEndpoint)

object Library:
  case class Author(name: String)
  case class Book(title: String, year: Int, author: Author)

  val books = List(
    Book("The Sorrows of Young Werther", 1774, Author("Johann Wolfgang von Goethe")),
    Book("On the Niemen", 1888, Author("Eliza Orzeszkowa")),
    Book("The Art of Computer Programming", 1968, Author("Donald Knuth")),
    Book("Pharaoh", 1897, Author("Boleslaw Prus"))
  )
