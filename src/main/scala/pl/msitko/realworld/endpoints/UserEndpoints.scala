package pl.msitko.realworld.endpoints

import io.circe.generic.auto.*
import io.circe.{Decoder, Encoder}
import pl.msitko.realworld.Entities.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object UserEndpoints:
  val authentication = endpoint.post
    .in("api" / "users" / "login")
    .in(jsonBody[AuthenticationReqBody])
    .out(jsonBody[UserBody])
    .out(statusCode(StatusCode.Created))

  val registration = endpoint.post
    .in("api" / "users")
    .in(jsonBody[RegistrationReqBody])
    .out(jsonBody[UserBody])
    .out(statusCode(StatusCode.Created))

  val getCurrentUser = endpoint.get
    .in("api" / "user")
    .out(jsonBody[UserBody])

  val updateUser = endpoint.put
    .in("api" / "user")
    .in(jsonBody[UpdateUserReqBody])
    .out(jsonBody[UserBody])
