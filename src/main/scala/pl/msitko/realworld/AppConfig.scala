package pl.msitko.realworld

import cats.effect.IO
import pureconfig.*
import pureconfig.generic.derivation.default.*
import scala.concurrent.duration.FiniteDuration

final case class AppConfig(
    server: ServerConfig,
    db: DatabaseConfig,
    jwt: JwtConfig,
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
) derives ConfigReader:
  override def toString: String =
    s"DatabaseConfig(host: $host, port: $port, dbName: $dbName, username: $username, password: <masked>)"

final case class JwtConfig(
    secret: String,
    expiration: FiniteDuration
) derives ConfigReader:
  override def toString: String =
    if (secret.length > 64)
      s"JwtConfig(secret: ${secret.take(4)}..., expiration: $expiration)"
    else
      s"JwtConfig(secret: <masked>, expiration: $expiration)"
