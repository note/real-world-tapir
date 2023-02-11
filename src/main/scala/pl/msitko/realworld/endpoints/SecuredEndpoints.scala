package pl.msitko.realworld.endpoints

import cats.implicits.*
import cats.effect.IO
import pl.msitko.realworld.{db, JWT, JwtConfig}
import pl.msitko.realworld.db.UserRepo
import sttp.model.StatusCode
import sttp.tapir.*

import java.time.Instant
import java.util.UUID
import scala.util.Success

class SecuredEndpoints(jwtConfig: JwtConfig):
  def authLogic(token: String): IO[Either[String, UUID]] =
    IO.pure {
      JWT.decodeJwtToken(token, jwtConfig) match
        case Success((userId, expirationDate)) if Instant.now().isBefore(expirationDate) => Right(userId)
        case _                                                                           => Left("TODO")
    }

  private val msg =
    "As per https://www.realworld.how/docs/specs/backend-specs/endpoints#authentication-header Authorization is supposed to start with 'Token '"
  private val expectedPrefix = "Token "
  val secureEndpoint = endpoint
    // As per https://www.realworld.how/docs/specs/backend-specs/endpoints#authentication-header
    .securityIn(
      header[String]("Authorization")
        .mapValidate[String](
          Validator.custom(
            s =>
              if (s.startsWith(expectedPrefix))
                ValidationResult.Valid
              else
                ValidationResult.Invalid(msg),
            msg.some))(s => s.stripPrefix(expectedPrefix))((token: String) => s"Token $token")
    )
    .errorOut(plainBody[String])
    .errorOut(statusCode(StatusCode.Unauthorized))
    .serverSecurityLogic(authLogic)
