package pl.msitko.realworld

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import java.time.Instant

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
