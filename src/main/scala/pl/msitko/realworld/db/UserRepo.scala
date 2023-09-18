package pl.msitko.realworld.db

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres._

class UserRepo(transactor: Transactor[IO]):
  def insert(user: UserNoId, password: String): IO[Either[String, User]] =
    sql"INSERT INTO public.users (email, password, username, bio, image) VALUES (${user.email}, crypt($password, gen_salt('bf', 11)), ${user.username}, ${user.bio}, ${user.image})".update
      .withUniqueGeneratedKeys[User]("id", "email", "username", "bio", "image")
      .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
        "Either email or username already exists"
      }
      .transact(transactor)

  def authenticate(email: String, password: String): IO[Option[User]] =
    sql"SELECT id, email, username, bio, image FROM public.users WHERE email=$email AND password=crypt($password, password)"
      .query[User]
      .option
      .transact(transactor)

  def getById(userId: UserId, subjectUserId: UserId): IO[Option[FullUser]] =
    sql"""WITH followerz AS (SELECT followed, COUNT(followed) count FROM followers WHERE follower=$subjectUserId AND followed=$userId GROUP BY followed)
         |                  SELECT id, email, username, bio, image, f.count FROM users
         |                         LEFT JOIN followerz f ON users.id = f.followed
         |                         WHERE id = $userId""".stripMargin
      .query[FullUser]
      .option
      .transact(transactor)

  def getById(userId: UserId): IO[Option[FullUser]] =
    sql"""SELECT id, email, username, bio, image, NULL FROM users WHERE id = $userId""".stripMargin
      .query[FullUser]
      .option
      .transact(transactor)

  def resolveUsername(username: String): IO[Option[UserId]] =
    sql"SELECT id from users where username=$username".query[UserId].option.transact(transactor)

  def updateUser(ch: UpdateUser, userId: UserId): IO[Either[String, Int]] =
    ch.password match
      case Some(newPassword) =>
        sql"""UPDATE users SET
             |email=${ch.email},
             |username=${ch.username},
             |password=crypt($newPassword, gen_salt('bf', 11)),
             |bio=${ch.bio},
             |image=${ch.image}
             |WHERE id=$userId
           """.stripMargin.update.run
          .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
            "Either email or username already exists"
          }
          .transact(transactor)
      case None =>
        sql"""UPDATE users SET
             |email=${ch.email},
             |username=${ch.username},
             |bio=${ch.bio},
             |image=${ch.image}
             |WHERE id=$userId
           """.stripMargin.update.run
          .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
            "Either email or username already exists"
          }
          .transact(transactor)
