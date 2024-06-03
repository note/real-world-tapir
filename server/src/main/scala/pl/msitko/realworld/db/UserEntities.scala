package pl.msitko.realworld.db

import cats.implicits.*
import doobie.*
import pl.msitko.realworld.entities
import pl.msitko.realworld.{db, Validated, Validation}

// TODO: introduce chimney-like type transformers? (as UserNoId and User are basically the same thing)
final case class UserNoId(
    email: String,
    username: String,
    bio: Option[String],
    image: Option[String]
)

final case class User(
    id: UserId,
    email: String,
    username: String,
    bio: Option[String],
    image: Option[String]
):
  def toHttp(jwtToken: String): entities.UserBody =
    entities.UserBody(user = entities.User(
      email = email,
      token = jwtToken,
      username = username,
      bio = bio,
      image = image,
    ))

final case class UpdateUser(
    email: String,
    username: String,
    password: Option[String],
    bio: Option[String],
    image: Option[String],
)

object UpdateUser:
  def fromHttp(req: entities.UpdateUserReqBody, existingUser: User): Validated[UpdateUser] =
    (
      req.user.email
        .map(newEmail => Validation.validEmail("user.email")(newEmail))
        .getOrElse(existingUser.email.validNec),
      req.user.username
        .map(newUserName => Validation.nonEmptyString("user.username")(newUserName))
        .getOrElse(existingUser.username.validNec),
      req.user.password
        .map(newPassword => Validation.nonEmptyString("user.password")(newPassword))
        .sequence,
    ).mapN { (email, username, password) =>
      db.UpdateUser(
        email = email,
        username = username,
        password = password,
        // TODO: Current treatment of bio and image is problematic in case of nullifying those values as part of update
        // On the other hand specs don't tell anything explicitly about nullifying. I guess something like following
        // would make sense:
        // Omitting value in update request means "no change"
        // Specifying value to be null explicitly means "change the value to null"
        bio = req.user.bio.orElse(existingUser.bio),
        image = req.user.image.orElse(existingUser.image)
      )
    }

final case class FullUser(
    user: User,
    followed: Boolean
):
  def toAuthor: Author =
    Author(
      username = user.username,
      bio = user.bio,
      image = user.bio,
      following = followed
    )

object FullUser:
  implicit val fullUserRead: Read[FullUser] =
    Read[(UserId, String, String, Option[String], Option[String], Option[Int])].map {
      case (id, email, username, bio, image, followed) =>
        FullUser(
          user = User(id = id, email = email, username = username, bio = bio, image = image),
          followed = followed.isDefined)
    }
