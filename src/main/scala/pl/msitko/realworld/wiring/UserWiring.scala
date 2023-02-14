package pl.msitko.realworld.wiring

import cats.effect.IO
import pl.msitko.realworld.Entities.UserBody
import pl.msitko.realworld.endpoints.UserEndpoints
import pl.msitko.realworld.services.UserService
import sttp.tapir.server.ServerEndpoint

object UserWiring:
  def endpoints(userEndpoints: UserEndpoints, service: UserService): List[ServerEndpoint[Any, IO]] =
    List(
      userEndpoints.authentication.serverLogic(service.authentication),
      userEndpoints.registration.serverLogicSuccess { reqBody =>
        for {
          // (insertedUser, jwtToken) <- service.registration
          // does not compile. Does it have something to do with https://github.com/lampepfl/dotty/issues/15579 ?
          t <- service.registration(reqBody)
        } yield UserBody.fromDB(t._1, t._2)
      },
      userEndpoints.getCurrentUser.serverLogic(userId => _ => service.getCurrentUser(userId)),
      userEndpoints.updateUser.serverLogic(service.updateUser),
    )
