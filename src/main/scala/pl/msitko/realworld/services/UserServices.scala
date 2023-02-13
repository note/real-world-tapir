package pl.msitko.realworld.services

import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import doobie.*
import doobie.implicits.*
import pl.msitko.realworld.Entities.UserBody
import pl.msitko.realworld.{Entities, ExampleResponses, JWT, JwtConfig}
import pl.msitko.realworld.db.{UserNoId, UserRepo}
import pl.msitko.realworld.endpoints.{ErrorInfo, UserEndpoints}
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UserServices(repo: UserRepo, jwtConfig: JwtConfig) extends StrictLogging:
  val userEndpoints = new UserEndpoints(jwtConfig)

  val authenticationImpl =
    userEndpoints.authentication.serverLogic { reqBody =>
      val encodedPassword = reqBody.user.password
      for {
        authenticated <- repo.authenticate(reqBody.user.email, encodedPassword)
        response = authenticated match
          case Some(user) =>
            Right(UserBody.fromDB(user, JWT.generateJwtToken(user.id.toString, jwtConfig)))
          case None =>
            Left(StatusCode.Forbidden)
      } yield response
    }

  val registrationImpl =
    userEndpoints.registration.serverLogicSuccess { reqBody =>
      val user            = reqBody.user
      val encodedPassword = reqBody.user.password
      for {
        inserted <- repo.insert(UserNoId(email = user.email, username = user.username, bio = user.bio), encodedPassword)
        httpUser = UserBody.fromDB(inserted, JWT.generateJwtToken(inserted.id.toString, jwtConfig))
      } yield httpUser
    }

  val getCurrentUserImpl =
    userEndpoints.getCurrentUser.serverLogic { userId => _ =>
      repo.getById(userId).flatMap {
        case Some(user) =>
          IO.pure(Right(UserBody.fromDB(user, JWT.generateJwtToken(user.id.toString, jwtConfig))))
        case None =>
          IO.pure(Left(ErrorInfo.NotFound))
      }
    }

  val updateUserImpl =
    userEndpoints.updateUser.serverLogicSuccess(_ => _ => IO.pure(ExampleResponses.userBody))

  val services = List(
    authenticationImpl,
    registrationImpl,
    getCurrentUserImpl,
    updateUserImpl,
  )
