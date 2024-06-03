package pl.msitko.realworld.db

import cats.effect.IO
import doobie.*
import doobie.implicits.*

class HealthRepo(transactor: Transactor[IO]):
  def testConnection(): IO[Int] =
    sql"SELECT 1".query[Int].unique.transact(transactor)
