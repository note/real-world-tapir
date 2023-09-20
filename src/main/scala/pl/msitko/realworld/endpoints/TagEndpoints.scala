package pl.msitko.realworld.endpoints

import io.circe.generic.auto.*
import pl.msitko.realworld.entities.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object TagEndpoints:
  val getTags = endpoint.get
    .in("api" / "tags")
    .out(jsonBody[Tags])
    .tag("tags")
