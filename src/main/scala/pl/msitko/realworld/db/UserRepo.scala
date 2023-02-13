package pl.msitko.realworld.db

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.implicits.legacy.instant.*
import pl.msitko.realworld.Entities.UserBody

import java.util.UUID

// TODO: introduce chimney-like type transformers? (as UserNoId and User are basically the same thing)
final case class UserNoId(
    email: String,
    username: String,
    bio: Option[String],
    image: Option[String]
)

final case class User(
    id: UUID, // TODO: introduce type tags for IDs?
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
)

class UserRepo(transactor: Transactor[IO]):
  implicit private val fullUserRead: Read[FullUser] =
    Read[(UUID, String, String, Option[String], Option[String], Option[Int])].map {
      case (id, email, username, bio, image, followed) =>
        FullUser(
          user = User(id = id, email = email, username = username, bio = bio, image = image),
          followed = followed.isDefined)
    }

  def insert(user: UserNoId, password: String): IO[User] =
    sql"INSERT INTO public.users (email, password, username, bio, image) VALUES (${user.email}, crypt($password, gen_salt('bf', 11)), ${user.username}, ${user.bio}, ${user.image})".update
      .withUniqueGeneratedKeys[User]("id", "email", "username", "bio", "image")
      .transact(transactor)

  def authenticate(email: String, password: String): IO[Option[User]] =
    sql"SELECT id, email, username, bio, image FROM public.users WHERE email=$email AND password=crypt($password, password)"
      .query[User]
      .option
      .transact(transactor)

  def getById(userId: UUID, subjectUserId: UUID): IO[Option[FullUser]] =
    sql"""WITH followerz AS (SELECT followed, COUNT(followed) count FROM followers WHERE follower=$subjectUserId AND followed=$userId GROUP BY followed)
         |                  SELECT id, email, username, bio, image, f.count FROM users
         |                         LEFT JOIN followerz f ON users.id = f.followed
         |                         WHERE id = $userId""".stripMargin
      .query[FullUser]
      .option
      .transact(transactor)

  def updateUser(ch: UpdateUser, userId: UUID): IO[Int] =
    ch.password match
      case Some(newPassword) =>
        sql"""UPDATE users SET
             |email=${ch.email},
             |username=${ch.username},
             |password=crypt($newPassword, gen_salt('bf', 11)),
             |bio=${ch.bio},
             |image=${ch.image}
             |WHERE id=$userId
           """.stripMargin.update.run.transact(transactor)
      case None =>
        sql"""UPDATE users SET
             |email=${ch.email},
             |username=${ch.username},
             |bio=${ch.bio},
             |image=${ch.image}
             |WHERE id=$userId
           """.stripMargin.update.run.transact(transactor)
