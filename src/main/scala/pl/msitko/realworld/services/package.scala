package pl.msitko.realworld

import cats.data.EitherT
import cats.effect.IO
import pl.msitko.realworld.endpoints.ErrorInfo

package object services {
  type Result[T] = EitherT[IO, ErrorInfo, T]
}
