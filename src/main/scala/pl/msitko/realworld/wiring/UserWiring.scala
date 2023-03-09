package pl.msitko.realworld.wiring

import cats.effect.IO
import pl.msitko.realworld.entities.OtherEntities.UserBody
import pl.msitko.realworld.endpoints.UserEndpoints
import pl.msitko.realworld.services.UserService
import sttp.tapir.server.ServerEndpoint

object UserWiring:
  def endpoints(userEndpoints: UserEndpoints, service: UserService): List[ServerEndpoint[Any, IO]] =
    List(
      userEndpoints.authentication.serverLogic(service.authentication),
      userEndpoints.registration.serverLogic(service.registration),
      userEndpoints.getCurrentUser.serverLogic(userId => _ => service.getCurrentUser(userId)),
      userEndpoints.updateUser.serverLogic(service.updateUser),
    )
