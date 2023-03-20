package pl.msitko.realworld.endpoints

import io.circe.generic.auto.*
import pl.msitko.realworld.entities.*
import pl.msitko.realworld.JwtConfig
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

class ProfileEndpoints(jwtConfig: JwtConfig) extends SecuredEndpoints(jwtConfig):
  val getProfile = optionallySecureEndpoint.get
    .in("api" / "profiles")
    .in(path[String].name("userId"))
    .out(jsonBody[ProfileBody])
    .tag("profiles")

  val followProfile = secureEndpoint.post
    .in("api" / "profiles")
    .in(path[String].name("userId"))
    .in("follow")
    .out(statusCode(StatusCode.Created))
    .out(jsonBody[ProfileBody])
    .tag("profiles")

  val unfollowProfile = secureEndpoint.delete
    .in("api" / "profiles")
    .in(path[String].name("userId"))
    .in("follow")
    .out(jsonBody[ProfileBody])
    .tag("profiles")
