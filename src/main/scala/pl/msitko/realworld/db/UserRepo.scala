package pl.msitko.realworld.db

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import pl.msitko.realworld.Entities.UserBody

// TODO: introduce chimney-like type transformers? (as UserNoId and User are basically the same thing)
final case class UserNoId(
    email: String,
    username: String,
    bio: Option[String]
)

final case class User(
    id: String, // TODO: introduce type tags for IDs?
    email: String,
    username: String,
    bio: Option[String]
)

class UserRepo(transactor: Transactor[IO]):
  def insert(user: UserNoId, password: String): IO[User] =
    val q: doobie.ConnectionIO[User] =
      sql"INSERT INTO public.users (email, password, username, bio) VALUES (${user.email}, crypt($password, gen_salt('bf', 11)), ${user.username}, ${user.bio})".update
        .withUniqueGeneratedKeys[User]("id", "email", "username", "bio")

    q.transact(transactor)

  def authenticate(email: String, password: String): IO[Option[User]] =
    sql"SELECT id, email, username, bio FROM public.users WHERE email=$email AND password=crypt($password, password)"
      .query[User]
      .option
      .transact(transactor)
