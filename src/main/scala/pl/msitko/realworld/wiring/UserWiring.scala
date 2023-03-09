package pl.msitko.realworld.wiring

import cats.effect.IO
import pl.msitko.realworld.endpoints.UserEndpoints
import pl.msitko.realworld.entities.UserBody
import pl.msitko.realworld.services.UserService
import sttp.tapir.server.ServerEndpoint

object UserWiring:
  def endpoints(userEndpoints: UserEndpoints, service: UserService): List[ServerEndpoint[Any, IO]] =
    List(
      userEndpoints.authentication.serverLogic(service.authentication),
      userEndpoints.registration.resultLogic(reqBody =>
        service.registration(reqBody).map((dbUser, jwtToken) => UserBody.fromDB(dbUser, jwtToken))),
      userEndpoints.getCurrentUser.serverLogic(userId => _ => service.getCurrentUser(userId)),
      userEndpoints.updateUser.resultLogic(service.updateUser),
    )
