package pl.msitko.realworld.services

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.all.*
import com.typesafe.scalalogging.StrictLogging
import pl.msitko.realworld.Entities.UserBody
import pl.msitko.realworld.{Entities, JWT, JwtConfig}
import pl.msitko.realworld.db
import pl.msitko.realworld.db.{FullUser, UpdateUser, User, UserNoId, UserRepo}
import pl.msitko.realworld.endpoints.{ErrorInfo, UserEndpoints}
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UserServices(repo: UserRepo, jwtConfig: JwtConfig) extends StrictLogging:
  val userEndpoints = new UserEndpoints(jwtConfig)
  val helper        = UserServicesHelper.fromRepo(repo)

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
      repo.getById(userId, userId).flatMap {
        case Some(user) =>
          IO.pure(Right(UserBody.fromDB(user.user, JWT.generateJwtToken(user.user.id.toString, jwtConfig))))
        case None =>
          IO.pure(Left(ErrorInfo.NotFound))
      }
    }

  val updateUserImpl =
    userEndpoints.updateUser.serverLogic { userId => reqBody =>
      (for {
        existingUser <- helper.getById(userId, userId)
        updateObj = reqBody.toDB(existingUser.user)
        _           <- helper.updateUser(updateObj, userId)
        updatedUser <- helper.getById(userId, userId)
      } yield UserBody.fromDB(updatedUser.user, JWT.generateJwtToken(userId.toString, jwtConfig))).value
    }

  val services = List(
    authenticationImpl,
    registrationImpl,
    getCurrentUserImpl,
    updateUserImpl,
  )

trait UserServicesHelper:
  def getById(userId: UUID, subjectUserId: UUID): Result[db.FullUser]
  def getById(userId: UUID): Result[db.FullUser]
  def updateUser(updateObj: db.UpdateUser, userId: UUID): Result[Unit]

object UserServicesHelper:
  def fromRepo(userRepo: UserRepo): UserServicesHelper =
    new UserServicesHelper:
      override def getById(userId: UUID, subjectUserId: UUID): Result[FullUser] =
        EitherT(userRepo.getById(userId, subjectUserId).map {
          case Some(user) => Right(user)
          case None       => Left(ErrorInfo.NotFound)
        })

      override def getById(userId: UUID): Result[db.FullUser] =
        EitherT(userRepo.getById(userId).map {
          case Some(user) => Right(user)
          case None       => Left(ErrorInfo.NotFound)
        })

      override def updateUser(updateObj: UpdateUser, userId: UUID): Result[Unit] =
        EitherT(userRepo.updateUser(updateObj, userId).map(_ => Right(())))
