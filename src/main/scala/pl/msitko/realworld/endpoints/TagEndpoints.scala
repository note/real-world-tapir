package pl.msitko.realworld.endpoints

import io.circe.generic.auto.*
import io.circe.{Decoder, Encoder}
import pl.msitko.realworld.Entities.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object TagEndpoints:
  val getTags = endpoint.get
    .in("api" / "tags")
    .out(jsonBody[Tags])
