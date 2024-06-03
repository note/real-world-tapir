package pl.msitko.realworld.wiring

import cats.effect.IO
import pl.msitko.realworld.{JWT, JwtConfig}
import pl.msitko.realworld.db.UserId
import pl.msitko.realworld.endpoints.{ErrorInfo, SecuredEndpoints}

import java.time.Instant
import scala.util.Success

class AuthLogic(jwtConfig: JwtConfig):
  def authLogic(token: String): IO[Either[ErrorInfo, UserId]] =
    IO.pure {
      JWT.decodeJwtToken(token, jwtConfig) match
        case Success((userId, expirationDate)) if Instant.now().isBefore(expirationDate) =>
          Right(userId)
        case _ => Left(ErrorInfo.Unauthenticated)
    }

  def optionalAuthLogic(tokenOpt: Option[String]): IO[Either[ErrorInfo, Option[UserId]]] =
    IO.pure {
      Right(tokenOpt.map(_.stripPrefix(SecuredEndpoints.expectedPrefix)).flatMap { token =>
        JWT.decodeJwtToken(token, jwtConfig) match
          case Success((userId, expirationDate)) if Instant.now().isBefore(expirationDate) =>
            Some(userId)
          case _ => None
      })
    }
