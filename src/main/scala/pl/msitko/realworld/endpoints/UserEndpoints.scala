package pl.msitko.realworld.endpoints

import io.circe.generic.auto.*
import pl.msitko.realworld.entities.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object UserEndpoints:
  val authentication: Endpoint[Unit, AuthenticationReqBody, StatusCode, UserBody, Any] = endpoint.post
    .in("api" / "users" / "login")
    .in(jsonBody[AuthenticationReqBody])
    .out(jsonBody[UserBody])
    .out(statusCode(StatusCode.Created))
    .errorOut(statusCode)
    .tag("users")

  val registration: Endpoint[Unit, RegistrationReqBody, ErrorInfo.ValidationError, UserBody, Any] = endpoint.post
    .in("api" / "users")
    .in(jsonBody[RegistrationReqBody])
    .out(jsonBody[UserBody])
    .out(statusCode(StatusCode.Created))
    .errorOut(statusCode(StatusCode.UnprocessableEntity).and(jsonBody[ErrorInfo.ValidationError]))
    .tag("users")

  val getCurrentUser = SecuredEndpoints.secureEndpoint.get
    .in("api" / "user")
    .out(jsonBody[UserBody])
    .tag("users")

  val updateUser = SecuredEndpoints.secureEndpoint.put
    .in("api" / "user")
    .in(jsonBody[UpdateUserReqBody])
    .out(jsonBody[UserBody])
    .tag("users")
