package pl.msitko.realworld

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import io.circe.parser.parse
import io.circe.syntax.*
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import pl.msitko.realworld.db.UserId

import java.time.Instant
import java.util.UUID
import scala.util.{Failure, Try}

final case class JWTContent(
    userId: String
)
object JWTContent:
  implicit val decoder: Decoder[JWTContent] = deriveDecoder[JWTContent]
  implicit val encoder: Encoder[JWTContent] = deriveEncoder[JWTContent]

object JWT:
  def generateJwtToken(userId: String, jwtConfig: JwtConfig): String =
    val content         = JWTContent(userId = userId)
    val contentAsString = content.asJson.noSpaces
    val claim = JwtClaim(
      content = contentAsString,
      expiration = Some(Instant.now.plusSeconds(jwtConfig.expiration.toSeconds).getEpochSecond),
      issuedAt = Some(Instant.now.getEpochSecond)
    )
    JwtCirce.encode(claim, jwtConfig.secret, JwtAlgorithm.HS256)

  def decodeJwtToken(token: String, jwtConfig: JwtConfig): Try[(UserId, Instant)] =
    JwtCirce.decode(token, jwtConfig.secret, Seq(JwtAlgorithm.HS256)).flatMap { claim =>
      (parse(claim.content).flatMap(_.as[JWTContent]), claim.expiration) match
        case Right(jwtContent) -> Some(expiration) =>
          Try(db.liftToUserId(UUID.fromString(jwtContent.userId))).map(uid => uid -> Instant.ofEpochSecond(expiration))
        case _ =>
          Failure(new RuntimeException("Either proper content or expiration missing in JWT token"))
    }
