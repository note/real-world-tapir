package pl.msitko.realworld

import cats.effect.IO
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

object DBMigration:
  def migrate(url: String, user: String, password: String): IO[MigrateResult] =
    IO {
      Flyway
        .configure()
        .dataSource(url, user, password)
        .load()
        .migrate()
    }
