package pl.msitko.realworld.db

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*

import java.util.UUID

final case class Follow(follower: UUID, followed: UUID)

class FollowRepo(transactor: Transactor[IO]):
  def insert(follow: Follow): IO[Int] =
    sql"INSERT INTO followers (follower, followed) VALUES (${follow.follower}, ${follow.followed})".update.run
      .transact(transactor)

  def delete(follow: Follow): IO[Int] =
    sql"DELETE FROM followers WHERE follower=${follow.follower} AND followed=${follow.followed}".update.run
      .transact(transactor)
