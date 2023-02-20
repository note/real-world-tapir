package pl.msitko.realworld.services

import cats.data.EitherT
import cats.effect.IO
import pl.msitko.realworld.Entities.ProfileBody
import pl.msitko.realworld.db
import pl.msitko.realworld.db.{Follow, FollowRepo, UserId, UserRepo}
import pl.msitko.realworld.endpoints.ErrorInfo

object ProfileService:
  def apply(repos: Repos) =
    new ProfileService(repos.followRepo, repos.userRepo)

class ProfileService(followRepo: FollowRepo, userRepo: UserRepo):
  private val userHelper = UserServicesHelper.fromRepo(userRepo)

  def getProfile(userIdOpt: Option[UserId])(profileName: String): IO[Either[ErrorInfo, ProfileBody]] =
    (for {
      requestedUserId <- resolveUserName(profileName)
      user            <- getByUserId(requestedUserId, userIdOpt)
    } yield ProfileBody.fromDB(user.toAuthor)).value

  def followProfile(userId: UserId)(profileName: String): IO[Either[ErrorInfo, ProfileBody]] =
    (for {
      userIdToFollow <- resolveUserName(profileName)
      update = db.Follow(follower = userId, followed = userIdToFollow)
      _    <- insertFollow(update)
      user <- userHelper.getById(userIdToFollow, subjectUserId = userId)
    } yield ProfileBody.fromDB(user.toAuthor)).value

  def unfollowProfile(userId: UserId)(profileName: String): IO[Either[ErrorInfo, ProfileBody]] =
    (for {
      userIdToFollow <- resolveUserName(profileName)
      update = db.Follow(follower = userId, followed = userIdToFollow)
      _    <- deleteFollow(update)
      user <- userHelper.getById(userIdToFollow, subjectUserId = userId)
    } yield ProfileBody.fromDB(user.toAuthor)).value

  private def resolveUserName(username: String): Result[UserId] =
    EitherT(userRepo.resolveUsername(username).map {
      case Some(uid) => Right(uid)
      case None      => Left(ErrorInfo.NotFound)
    })

  private def insertFollow(follow: Follow): Result[Int] =
    EitherT.right(followRepo.insert(follow))

  private def deleteFollow(follow: Follow): Result[Int] =
    EitherT.right(followRepo.delete(follow))

  private def getByUserId(userId: UserId, subjectUserId: Option[UserId]) =
    subjectUserId match
      case Some(uid) => userHelper.getById(userId, subjectUserId = uid)
      case None      => userHelper.getById(userId)
