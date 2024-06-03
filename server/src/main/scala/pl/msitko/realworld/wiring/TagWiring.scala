package pl.msitko.realworld.wiring

import cats.effect.IO
import pl.msitko.realworld.endpoints.TagEndpoints
import pl.msitko.realworld.services.TagService
import sttp.tapir.server.ServerEndpoint

object TagWiring:
  def endpoints(service: TagService): List[ServerEndpoint[Any, IO]] =
    List(
      TagEndpoints.getTags.serverLogicSuccess(_ => service.getTags)
    )
