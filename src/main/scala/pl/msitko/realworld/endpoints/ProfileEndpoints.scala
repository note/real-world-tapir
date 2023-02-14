package pl.msitko.realworld.endpoints

import io.circe.generic.auto.*
import pl.msitko.realworld.Entities.*
import pl.msitko.realworld.JwtConfig
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

class ProfileEndpoints(jwtConfig: JwtConfig) extends SecuredEndpoints(jwtConfig):
  val getProfile = optionallySecureEndpoint.get
    .in("api" / "profiles")
    .in(path[String])
    .out(jsonBody[ProfileBody])

  val followProfile = secureEndpoint.post
    .in("api" / "profiles")
    .in(path[String])
    .in("follow")
    .out(jsonBody[ProfileBody])

  val unfollowProfile = secureEndpoint.delete
    .in("api" / "profiles")
    .in(path[String])
    .in("follow")
    .out(jsonBody[ProfileBody])
