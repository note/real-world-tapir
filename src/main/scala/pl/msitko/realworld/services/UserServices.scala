package pl.msitko.realworld.services

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.all.*
import com.typesafe.scalalogging.StrictLogging
import pl.msitko.realworld.Entities.UserBody
import pl.msitko.realworld.{Entities, ExampleResponses, JWT, JwtConfig}
import pl.msitko.realworld.db
import pl.msitko.realworld.db.{User, UserNoId, UserRepo}
import pl.msitko.realworld.endpoints.{ErrorInfo, UserEndpoints}
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UserServices(repo: UserRepo, jwtConfig: JwtConfig) extends StrictLogging:
  // Move somewhere else
  type Result[T] = EitherT[IO, ErrorInfo, T]

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
        inserted <- repo.insert(
          UserNoId(email = user.email, username = user.username, bio = user.bio, image = user.image),
          encodedPassword)
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
    userEndpoints.updateUser.serverLogic { userId => reqBody =>
      (for {
        existingUser <- getById(userId)
        updateObj = reqBody.toDB(existingUser)
        _           <- updateUser(updateObj, userId)
        updatedUser <- getById(userId)
      } yield UserBody.fromDB(updatedUser, JWT.generateJwtToken(userId.toString, jwtConfig))).value
    }

  val services = List(
    authenticationImpl,
    registrationImpl,
    getCurrentUserImpl,
    updateUserImpl,
  )

  private def getById(userId: UUID): Result[db.User] =
    EitherT(repo.getById(userId).map {
      case Some(user) => Right(user)
      case None       => Left(ErrorInfo.NotFound)
    })

  private def updateUser(updateObj: db.UpdateUser, userId: UUID): Result[Unit] =
    EitherT(repo.updateUser(updateObj, userId).map(_ => Right(())))
