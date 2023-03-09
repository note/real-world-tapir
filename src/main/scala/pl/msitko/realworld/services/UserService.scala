package pl.msitko.realworld.services

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.all.*
import com.typesafe.scalalogging.StrictLogging
import pl.msitko.realworld.*
import pl.msitko.realworld.entities.OtherEntities.{AuthenticationReqBody, RegistrationReqBody, UpdateUserReqBody, UserBody}
import pl.msitko.realworld.{JWT, JwtConfig, Validated, db}
import pl.msitko.realworld.db.{UserId, UserRepo}
import pl.msitko.realworld.endpoints.ErrorInfo
import pl.msitko.realworld.entities.{AuthenticationReqBody, OtherEntities, RegistrationReqBody, UserBody}
import sttp.model.StatusCode

object UserService:
  def apply(repos: Repos, jwtConfig: JwtConfig) =
    new UserService(repos.userRepo, jwtConfig)

class UserService(repo: UserRepo, jwtConfig: JwtConfig) extends StrictLogging:
  private val helper = UserServicesHelper.fromRepo(repo)
  // Copied from https://itecnote.com/tecnote/r-validate-email-one-liner-in-scala/
  private val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

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

  def registration(reqBody: RegistrationReqBody): IO[Either[ErrorInfo.ValidationError, UserBody]] =
    val password = reqBody.user.password
    (for {
      dbUser   <- validateRegistration(reqBody).toResult
      inserted <- EitherT.right(repo.insert(dbUser, password))
    } yield UserBody.fromDB(inserted, JWT.generateJwtToken(inserted.id.toString, jwtConfig))).value

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

  private def validateRegistration(reqBody: RegistrationReqBody): Validated[db.UserNoId] =
    val user = reqBody.user

    def validateEmail: Validated[String] =
      Validation.nonEmptyString("user.email")(user.email).andThen {
        case e if emailRegex.findFirstIn(e).isEmpty => ("user.email" -> "is not a valid email").invalidNec
        case e                                      => e.validNec
      }
    def validateUsername: Validated[String] =
      Validation.nonEmptyString("user.username")(user.username)
    def validatePassword: Validated[String] =
      Validation.nonEmptyString("user.password")(user.password)

    (validateEmail, validateUsername, validatePassword).mapN((email, username, _) =>
      db.UserNoId(email = email, username = username, bio = user.bio, image = user.image))

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
