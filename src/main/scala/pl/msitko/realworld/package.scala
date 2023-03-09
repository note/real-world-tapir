package pl.msitko

import cats.data.Validated.{Invalid, Valid}
import cats.data.{EitherT, ValidatedNec}
import cats.effect.IO
import cats.syntax.all.*
import pl.msitko.realworld.endpoints.ErrorInfo
import pl.msitko.realworld.endpoints.ErrorInfo.ValidationError
import pl.msitko.realworld.services.Result

package object realworld:
  type Validated[T] = ValidatedNec[(String, String), T]

  private def toRes[T](in: Validated[T]): EitherT[IO, ErrorInfo.ValidationError, T] = in match
    case Valid(v)   => EitherT.right[ErrorInfo.ValidationError](IO.pure(v))
    case Invalid(e) => EitherT.left[T](IO.pure(ValidationError.fromNec(e)))

  extension [T](v: Validated[T]) def toResult: EitherT[IO, ErrorInfo.ValidationError, T] = toRes(v)

  object Validation:
    def nonEmptyString(fieldName: String)(in: String): Validated[String] =
      if in.isEmpty then (fieldName -> "can't be empty").invalidNec else in.validNec
