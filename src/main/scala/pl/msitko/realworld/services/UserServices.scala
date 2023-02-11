package pl.msitko.realworld.services

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import pl.msitko.realworld.Entities.UserBody
import pl.msitko.realworld.ExampleResponses
import pl.msitko.realworld.endpoints.UserEndpoints
import sttp.tapir.server.ServerEndpoint

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UserServices(transactor: Transactor[IO]):
  val authenticationImpl =
    UserEndpoints.authentication.serverLogicSuccess(_ => IO.pure(ExampleResponses.userBody))

  val registrationImpl =
    UserEndpoints.registration.serverLogicSuccess { reqBody =>
      val user = reqBody.user
      // TODO: hash password

      val salt = Array.fill[Byte](16)(0)
      new SecureRandom().nextBytes(salt)
      val spec            = new PBEKeySpec(user.password.toCharArray, salt, 65536, 128)
      val factory         = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
      val encodedPassword = factory.generateSecret(spec).getEncoded

      val q: doobie.ConnectionIO[UserBody] =
        sql"INSERT INTO public.users (email, password, username, bio) VALUES (${user.email}, $encodedPassword, ${user.username}, ${user.bio})".update
          .withUniqueGeneratedKeys[UserBody]("id", "email", "password", "username", "bio")

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
