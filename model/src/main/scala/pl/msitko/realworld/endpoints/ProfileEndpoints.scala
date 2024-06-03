package pl.msitko.realworld.endpoints

import io.circe.generic.auto.*
import pl.msitko.realworld.entities.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object ProfileEndpoints:
  val getProfile = SecuredEndpoints.optionallySecureEndpoint.get
    .in("api" / "profiles")
    .in(path[String].name("userId"))
    .out(jsonBody[ProfileBody])
    .tag("profiles")

  val followProfile = SecuredEndpoints.secureEndpoint.post
    .in("api" / "profiles")
    .in(path[String].name("userId"))
    .in("follow")
    .out(statusCode(StatusCode.Created))
    .out(jsonBody[ProfileBody])
    .tag("profiles")

  val unfollowProfile = SecuredEndpoints.secureEndpoint.delete
    .in("api" / "profiles")
    .in(path[String].name("userId"))
    .in("follow")
    .out(jsonBody[ProfileBody])
    .tag("profiles")
