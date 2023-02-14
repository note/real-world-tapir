package pl.msitko.realworld.endpoints

import io.circe.generic.auto.*
import pl.msitko.realworld.Entities.*
import pl.msitko.realworld.JwtConfig
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

class UserEndpoints(jwtConfig: JwtConfig) extends SecuredEndpoints(jwtConfig):
  val authentication = endpoint.post
    .in("api" / "users" / "login")
    .in(jsonBody[AuthenticationReqBody])
    .out(jsonBody[UserBody])
    .out(statusCode(StatusCode.Created))
    .errorOut(statusCode)

  val registration = endpoint.post
    .in("api" / "users")
    .in(jsonBody[RegistrationReqBody])
    .out(jsonBody[UserBody])
    .out(statusCode(StatusCode.Created))

  val getCurrentUser = secureEndpoint.get
    .in("api" / "user")
    .out(jsonBody[UserBody])

  val updateUser = secureEndpoint.put
    .in("api" / "user")
    .in(jsonBody[UpdateUserReqBody])
    .out(jsonBody[UserBody])
