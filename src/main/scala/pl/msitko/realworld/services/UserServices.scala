package pl.msitko.realworld.services

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import pl.msitko.realworld.Entities.UserBody
import pl.msitko.realworld.ExampleResponses
import pl.msitko.realworld.endpoints.UserEndpoints
import sttp.tapir.server.ServerEndpoint

class UserServices(transactor: Transactor[IO]):
  val authenticationImpl =
    UserEndpoints.authentication.serverLogicSuccess(_ => IO.pure(ExampleResponses.userBody))

  val registrationImpl =
    UserEndpoints.registration.serverLogicSuccess { reqBody =>
      val user = reqBody.user
      // TODO: hash password
      val q: doobie.ConnectionIO[UserBody] =
        sql"INSERT INTO public.users (email, token, username, bio) VALUES (${user.email}, ${user.password}, ${user.username}, ${user.bio})".update
          .withUniqueGeneratedKeys[UserBody]("id", "email", "token", "username", "bio")

      q.transact(transactor)
    }

  val getCurrentUserImpl =
    UserEndpoints.getCurrentUser.serverLogicSuccess(_ => IO.pure(ExampleResponses.userBody))

  val updateUserImpl =
    UserEndpoints.updateUser.serverLogicSuccess(_ => IO.pure(ExampleResponses.userBody))

  val services = List(
    authenticationImpl,
    registrationImpl,
    getCurrentUserImpl,
    updateUserImpl,
  )
