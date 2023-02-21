package pl.msitko.realworld

import cats.data.EitherT
import cats.effect.IO
import pl.msitko.realworld.db
import pl.msitko.realworld.endpoints.ErrorInfo

import java.util.UUID

package object services:
  type Result[T] = EitherT[IO, ErrorInfo, T]

  private type AuthenticatedUserIdTag
  type AuthenticatedUserId = AuthenticatedUserIdTag & UUID
  def liftToAuthenticatedUserId(uuid: UUID): AuthenticatedUserId =
    uuid.asInstanceOf[AuthenticatedUserId]

  given Conversion[AuthenticatedUserId, db.UserId] with
    def apply(authUserId: AuthenticatedUserId): db.UserId =
      db.liftToUserId(authUserId)
