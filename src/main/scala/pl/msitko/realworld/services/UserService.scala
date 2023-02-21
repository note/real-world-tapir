package pl.msitko.realworld.services

import cats.data.EitherT
import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import pl.msitko.realworld.Entities.{AuthenticationReqBody, RegistrationReqBody, UpdateUserReqBody, UserBody}
import pl.msitko.realworld.{Entities, JWT, JwtConfig}
import pl.msitko.realworld.db
import pl.msitko.realworld.db.{UserId, UserRepo}
import pl.msitko.realworld.endpoints.ErrorInfo
import sttp.model.StatusCode

object UserService:
  def apply(repos: Repos, jwtConfig: JwtConfig) =
    new UserService(repos.userRepo, jwtConfig)

class UserService(repo: UserRepo, jwtConfig: JwtConfig) extends StrictLogging:
  private val helper = UserServicesHelper.fromRepo(repo)

  def authentication(reqBody: AuthenticationReqBody) =
    val encodedPassword = reqBody.user.password
    for {
      authenticated <- repo.authenticate(reqBody.user.email, encodedPassword)
      response = authenticated match
        case Some(user) =>
          Right(UserBody.fromDB(user, JWT.generateJwtToken(user.id.toString, jwtConfig)))
        case None =>
          Left(StatusCode.Forbidden)
    } yield response

  def registration(reqBody: RegistrationReqBody): IO[(db.User, String)] =
    val user            = reqBody.user
    val encodedPassword = reqBody.user.password
    for {
      inserted <- repo.insert(
        db.UserNoId(email = user.email, username = user.username, bio = user.bio, image = user.image),
        encodedPassword)

    } yield inserted -> JWT.generateJwtToken(inserted.id.toString, jwtConfig)

  def getCurrentUser(userId: UserId): IO[Either[ErrorInfo.NotFound.type, UserBody]] =
    repo.getById(userId, userId).flatMap {
      case Some(user) =>
        IO.pure(Right(UserBody.fromDB(user.user, JWT.generateJwtToken(user.user.id.toString, jwtConfig))))
      case None =>
        IO.pure(Left(ErrorInfo.NotFound))
    }

  def updateUser(userId: UserId)(reqBody: UpdateUserReqBody): IO[Either[ErrorInfo, UserBody]] =
    (for {
      existingUser <- helper.getById(userId, userId)
      updateObj = reqBody.toDB(existingUser.user)
      _           <- helper.updateUser(updateObj, userId)
      updatedUser <- helper.getById(userId, userId)
    } yield UserBody.fromDB(updatedUser.user, JWT.generateJwtToken(userId.toString, jwtConfig))).value

trait UserServicesHelper:
  def getById(userId: UserId, subjectUserId: UserId): Result[db.FullUser]
  def getById(userId: UserId): Result[db.FullUser]
  def updateUser(updateObj: db.UpdateUser, userId: UserId): Result[Unit]

object UserServicesHelper:
  def fromRepo(userRepo: UserRepo): UserServicesHelper =
    new UserServicesHelper:
      override def getById(userId: UserId, subjectUserId: UserId): Result[db.FullUser] =
        EitherT(userRepo.getById(userId, subjectUserId).map {
          case Some(user) => Right(user)
          case None       => Left(ErrorInfo.NotFound)
        })

      override def getById(userId: UserId): Result[db.FullUser] =
        EitherT(userRepo.getById(userId).map {
          case Some(user) => Right(user)
          case None       => Left(ErrorInfo.NotFound)
        })

      override def updateUser(updateObj: db.UpdateUser, userId: UserId): Result[Unit] =
        EitherT(userRepo.updateUser(updateObj, userId).map(_ => Right(())))