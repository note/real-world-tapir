package pl.msitko.realworld.db

import doobie.*
import doobie.implicits.*

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
)

final case class UpdateUser(
    email: String,
    username: String,
    password: Option[String],
    bio: Option[String],
    image: Option[String],
)

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
