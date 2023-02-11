package pl.msitko.realworld.services

import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import doobie.*
import doobie.implicits.*
import pl.msitko.realworld.Entities.UserBody
import pl.msitko.realworld.{Entities, ExampleResponses, JWT, JwtConfig}
import pl.msitko.realworld.db.{UserNoId, UserRepo}
import pl.msitko.realworld.endpoints.UserEndpoints
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UserServices(repo: UserRepo, jwtConfig: JwtConfig) extends StrictLogging:
  val authenticationImpl =
    UserEndpoints.authentication.serverLogic { reqBody =>
      val encodedPassword = reqBody.user.password
      for {
        authenticated <- repo.authenticate(reqBody.user.email, encodedPassword)
        response = authenticated match
          case Some(user) =>
            Right(UserBody.fromDB(user, JWT.generateJwtToken(user.id, jwtConfig)))
          case None =>
            Left(StatusCode.Forbidden)
      } yield response
    }

  val registrationImpl =
    UserEndpoints.registration.serverLogicSuccess { reqBody =>
      val user            = reqBody.user
      val encodedPassword = reqBody.user.password
      for {
        inserted <- repo.insert(UserNoId(email = user.email, username = user.username, bio = user.bio), encodedPassword)
        httpUser = UserBody.fromDB(inserted, JWT.generateJwtToken(inserted.id, jwtConfig))
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
