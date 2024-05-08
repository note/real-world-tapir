package pl.msitko.realworld.wiring

import cats.effect.IO
import pl.msitko.realworld.endpoints.{AuthLogic, UserEndpoints}
import pl.msitko.realworld.entities.UserBody
import pl.msitko.realworld.services.UserService
import sttp.tapir.server.ServerEndpoint

class UserWiring(authLogic: AuthLogic):
  def endpoints(service: UserService): List[ServerEndpoint[Any, IO]] =
    List(
      UserEndpoints.registration.resultLogic(reqBody =>
        service.registration(reqBody).map((dbUser, jwtToken) => UserBody.fromDB(dbUser, jwtToken))),
      UserEndpoints.authentication.serverLogic(service.authentication),
      UserEndpoints.getCurrentUser
        .serverSecurityLogic(authLogic.authLogic)
        .serverLogic(userId => _ => service.getCurrentUser(userId)),
      UserEndpoints.updateUser
        .serverSecurityLogic(authLogic.authLogic)
        .resultLogic(service.updateUser),
    )
