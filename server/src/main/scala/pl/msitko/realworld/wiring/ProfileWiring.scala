package pl.msitko.realworld.wiring

import cats.effect.IO
import pl.msitko.realworld.endpoints.ProfileEndpoints
import pl.msitko.realworld.services.ProfileService
import sttp.tapir.server.ServerEndpoint

class ProfileWiring(authLogic: AuthLogic):
  def endpoints(service: ProfileService): List[ServerEndpoint[Any, IO]] =
    List(
      ProfileEndpoints.getProfile.serverSecurityLogic(authLogic.optionalAuthLogic).resultLogic(service.getProfile),
      ProfileEndpoints.followProfile.serverSecurityLogic(authLogic.authLogic).resultLogic(service.followProfile),
      ProfileEndpoints.unfollowProfile.serverSecurityLogic(authLogic.authLogic).resultLogic(service.unfollowProfile),
    )
