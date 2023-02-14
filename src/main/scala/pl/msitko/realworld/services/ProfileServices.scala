package pl.msitko.realworld.services

import cats.data.EitherT
import pl.msitko.realworld.Entities.ProfileBody
import pl.msitko.realworld.db
import pl.msitko.realworld.db.{Follow, FollowRepo, UserRepo}
import pl.msitko.realworld.JwtConfig
import pl.msitko.realworld.endpoints.{ErrorInfo, ProfileEndpoints}

import java.util.UUID

class ProfileServices(followRepo: FollowRepo, userRepo: UserRepo, jwtConfig: JwtConfig):
  private val profileEndpoints = new ProfileEndpoints(jwtConfig)
  private val userHelper       = UserServicesHelper.fromRepo(userRepo)

  val getProfileImpl =
    profileEndpoints.getProfile.serverLogic { userIdOpt => profileName =>
      (for {
        requestedUserId <- resolveUserName(profileName)
        user            <- getByUserId(requestedUserId, userIdOpt)
      } yield ProfileBody.fromDB(user.toAuthor)).value
    }

  val followProfileImpl =
    profileEndpoints.followProfile.serverLogic { userId => profileName =>
      (for {
        userIdToFollow <- resolveUserName(profileName)
        update = db.Follow(follower = userId, followed = userIdToFollow)
        _    <- insertFollow(update)
        user <- userHelper.getById(userIdToFollow, subjectUserId = userId)
      } yield ProfileBody.fromDB(user.toAuthor)).value
    }

  val unfollowProfileImpl =
    profileEndpoints.unfollowProfile.serverLogic { userId => profileName =>
      (for {
        userIdToFollow <- resolveUserName(profileName)
        update = db.Follow(follower = userId, followed = userIdToFollow)
        _    <- deleteFollow(update)
        user <- userHelper.getById(userIdToFollow, subjectUserId = userId)
      } yield ProfileBody.fromDB(user.toAuthor)).value
    }

  val services = List(
    getProfileImpl,
    followProfileImpl,
    unfollowProfileImpl,
  )

  private def resolveUserName(username: String): Result[UUID] =
    EitherT(userRepo.resolveUsername(username).map {
      case Some(uid) => Right(uid)
      case None      => Left(ErrorInfo.NotFound)
    })

  private def insertFollow(follow: Follow): Result[Int] =
    EitherT.right(followRepo.insert(follow))

  private def deleteFollow(follow: Follow): Result[Int] =
    EitherT.right(followRepo.delete(follow))

  private def getByUserId(userId: UUID, subjectUserId: Option[UUID]) =
    subjectUserId match
      case Some(uid) => userHelper.getById(userId, subjectUserId = uid)
      case None      => userHelper.getById(userId)
