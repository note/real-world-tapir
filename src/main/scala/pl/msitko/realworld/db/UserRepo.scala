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

class UserRepo(transactor: Transactor[IO]):
  def insert(user: UserNoId, password: String): IO[User] =
    sql"INSERT INTO public.users (email, password, username, bio, image) VALUES (${user.email}, crypt($password, gen_salt('bf', 11)), ${user.username}, ${user.bio}, ${user.image})".update
      .withUniqueGeneratedKeys[User]("id", "email", "username", "bio", "image")
      .transact(transactor)

  def authenticate(email: String, password: String): IO[Option[User]] =
    sql"SELECT id, email, username, bio, image FROM public.users WHERE email=$email AND password=crypt($password, password)"
      .query[User]
      .option
      .transact(transactor)

  def getById(userId: UUID): IO[Option[User]] =
    sql"SELECT id, email, username, bio, image FROM public.users WHERE id=$userId"
      .query[User]
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
