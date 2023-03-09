package pl.msitko.realworld.services

import cats.data.EitherT
import pl.msitko.realworld.entities.ProfileBody
import pl.msitko.realworld.db
import pl.msitko.realworld.db.{Follow, FollowRepo, UserId, UserRepo}
import pl.msitko.realworld.endpoints.ErrorInfo

object ProfileService:
  def apply(repos: Repos) =
    new ProfileService(repos.followRepo, repos.userRepo)

class ProfileService(followRepo: FollowRepo, userRepo: UserRepo):
  private val userHelper = UserServicesHelper.fromRepo(userRepo)

  def followProfile(userId: UserId)(profileName: String): Result[ProfileBody] =
    for {
      userIdToFollow <- resolveUserName(profileName)
      update = db.Follow(follower = userId, followed = userIdToFollow)
      _    <- insertFollow(update)
      user <- userHelper.getById(userIdToFollow, subjectUserId = userId)
    } yield ProfileBody.fromDB(user.toAuthor)

  def getProfile(userIdOpt: Option[UserId])(profileName: String): Result[ProfileBody] =
    for {
      requestedUserId <- resolveUserName(profileName)
      user            <- getByUserId(requestedUserId, userIdOpt)
    } yield ProfileBody.fromDB(user.toAuthor)

  def unfollowProfile(userId: UserId)(profileName: String): Result[ProfileBody] =
    for {
      userIdToFollow <- resolveUserName(profileName)
      update = db.Follow(follower = userId, followed = userIdToFollow)
      _    <- deleteFollow(update)
      user <- userHelper.getById(userIdToFollow, subjectUserId = userId)
    } yield ProfileBody.fromDB(user.toAuthor)

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
