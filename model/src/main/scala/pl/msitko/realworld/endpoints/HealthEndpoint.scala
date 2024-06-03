package pl.msitko.realworld.endpoints

import io.circe.generic.auto.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

final case class HealthResponse(
    version: String,
    available: Boolean,
    dbAvailable: Boolean,
)

object HealthEndpoint:
  val health: Endpoint[Unit, Unit, Unit, HealthResponse, Any] =
    endpoint.get.in("api" / "health").out(jsonBody[HealthResponse]).tag("other")
