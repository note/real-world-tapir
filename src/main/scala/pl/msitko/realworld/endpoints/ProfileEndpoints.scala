package pl.msitko.realworld.endpoints

import io.circe.generic.auto.*
import io.circe.{Decoder, Encoder}
import pl.msitko.realworld.Entities.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object ProfileEndpoints:
  val getProfile = endpoint.get
    .in("api" / "profiles")
    .in(path[String])
    .out(jsonBody[ProfileBody])

  val followProfile = endpoint.post
    .in("api" / "profiles")
    .in(path[String])
    .in("follow")
    .out(jsonBody[ProfileBody])

  val unfollowProfile = endpoint.delete
    .in("api" / "profiles")
    .in(path[String])
    .in("follow")
    .out(jsonBody[ProfileBody])
