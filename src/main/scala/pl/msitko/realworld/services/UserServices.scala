package pl.msitko.realworld.services

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import pl.msitko.realworld.Entities.UserBody
import pl.msitko.realworld.{Entities, ExampleResponses, JWT, JwtConfig}
import pl.msitko.realworld.db.{UserNoId, UserRepo}
import pl.msitko.realworld.endpoints.UserEndpoints
import sttp.tapir.server.ServerEndpoint

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UserServices(repo: UserRepo, jwtConfig: JwtConfig):
  val authenticationImpl =
    UserEndpoints.authentication.serverLogicSuccess(_ => IO.pure(ExampleResponses.userBody))

  val registrationImpl =
    UserEndpoints.registration.serverLogicSuccess { reqBody =>
      val user            = reqBody.user
      val encodedPassword = hashPassword(user.password.toCharArray)
      for {
        inserted <- repo.insert(UserNoId(email = user.email, username = user.username, bio = user.bio), encodedPassword)
        httpUser = UserBody(user = Entities.User(
          email = inserted.email,
          token = JWT.generateJwtToken(inserted.id, jwtConfig),
          username = inserted.username,
          bio = inserted.bio,
          image = None,
        ))
      } yield httpUser
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

  private def hashPassword(in: Array[Char]): Array[Byte] =
    val salt = Array.fill[Byte](16)(0)
    new SecureRandom().nextBytes(salt)
    val spec    = new PBEKeySpec(in, salt, 65536, 128)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    factory.generateSecret(spec).getEncoded
