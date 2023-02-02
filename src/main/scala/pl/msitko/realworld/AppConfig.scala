package pl.msitko.realworld

import cats.effect.IO
import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class AppConfig(
    server: ServerConfig,
    db: DatabaseConfig,
) derives ConfigReader

object AppConfig:
  def loadConfig: IO[AppConfig] =
    IO(ConfigSource.default.loadOrThrow[AppConfig])

final case class ServerConfig(
    host: String,
    port: Int,
) derives ConfigReader

final case class DatabaseConfig(
    host: String,
    port: Int,
    dbName: String,
    username: String,
    password: String,
) derives ConfigReader
