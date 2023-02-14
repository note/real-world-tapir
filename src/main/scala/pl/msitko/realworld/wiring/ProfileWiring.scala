package pl.msitko.realworld.wiring

import cats.effect.IO
import pl.msitko.realworld.endpoints.ProfileEndpoints
import pl.msitko.realworld.services.ProfileService
import sttp.tapir.server.ServerEndpoint

object ProfileWiring:
  def endpoints(profileEndpoints: ProfileEndpoints, service: ProfileService): List[ServerEndpoint[Any, IO]] =
    List(
      profileEndpoints.getProfile.serverLogic(service.getProfile),
      profileEndpoints.followProfile.serverLogic(service.followProfile),
      profileEndpoints.unfollowProfile.serverLogic(service.unfollowProfile),
    )
