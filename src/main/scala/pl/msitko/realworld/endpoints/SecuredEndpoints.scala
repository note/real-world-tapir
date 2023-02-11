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

// TODO: move somewhere else:
sealed trait ErrorInfo
object ErrorInfo:
  case object NotFound        extends ErrorInfo
  case object Unauthorized    extends ErrorInfo
  case object Unauthenticated extends ErrorInfo

class SecuredEndpoints(jwtConfig: JwtConfig):
  def authLogic(token: String): IO[Either[ErrorInfo, UUID]] =
    IO.pure {
      JWT.decodeJwtToken(token, jwtConfig) match
        case Success((userId, expirationDate)) if Instant.now().isBefore(expirationDate) => Right(userId)
        case _ => Left(ErrorInfo.Unauthenticated)
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
    .errorOut(
      oneOf[ErrorInfo](
        oneOfVariant(statusCode(StatusCode.NotFound).and(emptyOutputAs(ErrorInfo.NotFound))),
        oneOfVariant(statusCode(StatusCode.Forbidden).and(emptyOutputAs(ErrorInfo.Unauthorized))),
        oneOfVariant(statusCode(StatusCode.Unauthorized).and(emptyOutputAs(ErrorInfo.Unauthenticated))),
      ))
    .serverSecurityLogic(authLogic)
