package pl.msitko.realworld.db

import cats.effect.IO
import doobie.*
import doobie.implicits.*

final case class Follow(follower: UserId, followed: UserId)

class FollowRepo(transactor: Transactor[IO]):
  def insert(follow: Follow): IO[Int] =
    sql"INSERT INTO followers (follower, followed) VALUES (${follow.follower}, ${follow.followed})".update.run
      .transact(transactor)

  def delete(follow: Follow): IO[Int] =
    sql"DELETE FROM followers WHERE follower=${follow.follower} AND followed=${follow.followed}".update.run
      .transact(transactor)

  def getFollowedByUser(userId: UserId): IO[List[UserId]] =
    sql"SELECT followed FROM followers WHERE follower=${userId}".query[UserId].to[List].transact(transactor)
