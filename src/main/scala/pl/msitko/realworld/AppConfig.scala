package pl.msitko.realworld

import cats.effect.IO
import com.comcast.ip4s.{Host, Port}
import pureconfig.*
import pureconfig.error.CannotConvert
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
    host: Host,
    port: Port,
) derives ConfigReader

object ServerConfig:
  given ConfigReader[Host] =
    ConfigReader.fromString(s => Host.fromString(s).toRight(CannotConvert(s, "Host", "cannot convert")))
  given ConfigReader[Port] =
    ConfigReader.fromString(s => Port.fromString(s).toRight(CannotConvert(s, "Port", "cannot convert")))

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
