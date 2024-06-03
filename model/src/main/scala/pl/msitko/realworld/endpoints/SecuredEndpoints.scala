package pl.msitko.realworld.endpoints

import cats.data.NonEmptyChain
import cats.implicits.*
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

// TODO: move somewhere else:
sealed trait ErrorInfo
object ErrorInfo:
  case object NotFound                                                extends ErrorInfo
  case object Unauthorized                                            extends ErrorInfo
  case object Unauthenticated                                         extends ErrorInfo
  final case class ValidationError(errors: Map[String, List[String]]) extends ErrorInfo
  object ValidationError:
    def fromNec(in: cats.data.NonEmptyChain[(String, String)]): ValidationError =
      val errs = in.toChain.toList.groupBy(_._1).map((k, v) => k -> v.map(_._2))
      ValidationError(errs)
    def fromTuple(in: (String, String)): ValidationError =
      ValidationError(Map(in._1 -> List(in._2)))

object SecuredEndpoints:
  private val msg =
    "As per https://www.realworld.how/docs/specs/backend-specs/endpoints#authentication-header Authorization is supposed to start with 'Token '"
  val expectedPrefix = "Token "
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
        oneOfVariant(statusCode(StatusCode.UnprocessableEntity).and(jsonBody[ErrorInfo.ValidationError])),
        oneOfVariant(statusCode(StatusCode.NotFound).and(emptyOutputAs(ErrorInfo.NotFound))),
        oneOfVariant(statusCode(StatusCode.Forbidden).and(emptyOutputAs(ErrorInfo.Unauthorized))),
        oneOfVariant(statusCode(StatusCode.Unauthorized).and(emptyOutputAs(ErrorInfo.Unauthenticated))),
      ))

  val optionallySecureEndpoint = endpoint
    // As per https://www.realworld.how/docs/specs/backend-specs/endpoints#authentication-header
    .securityIn(header[Option[String]]("Authorization"))
    .errorOut(
      oneOf[ErrorInfo](
        oneOfVariant(statusCode(StatusCode.NotFound).and(emptyOutputAs(ErrorInfo.NotFound))),
        oneOfVariant(statusCode(StatusCode.Forbidden).and(emptyOutputAs(ErrorInfo.Unauthorized))),
        oneOfVariant(statusCode(StatusCode.Unauthorized).and(emptyOutputAs(ErrorInfo.Unauthenticated))),
      ))
